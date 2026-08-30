package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncPayloadFactory
import com.raysix.fitns.core.sync.SyncQueueWriter
import com.raysix.fitns.core.undo.AppUndoRedoManager
import com.raysix.fitns.core.undo.UndoRedoAction
import com.raysix.fitns.data.local.dao.FoodDao
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.FoodNutrientEntity
import com.raysix.fitns.data.local.entity.FoodProductEntity
import com.raysix.fitns.data.local.entity.FoodServingEntity
import com.raysix.fitns.data.local.entity.SavedMealEntity
import com.raysix.fitns.data.local.entity.SavedMealItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.model.CustomFood
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.model.SavedMeal
import com.raysix.fitns.domain.model.SavedMealItem
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.usecase.MealScaler
import com.raysix.fitns.domain.usecase.NutritionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import com.raysix.fitns.data.local.FitNsDatabase
import androidx.room.withTransaction

private const val DateRefreshIntervalMillis = 60_000L

class LocalNutritionRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val database: FitNsDatabase,
    private val nutritionCalculator: NutritionCalculator,
    private val syncQueueWriter: SyncQueueWriter,
    private val syncPayloadFactory: SyncPayloadFactory,
    private val profileRepository: ProfileRepository,
    private val mealScaler: MealScaler,
    private val undoRedoManager: AppUndoRedoManager
) : NutritionRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeToday(): Flow<DailyNutritionDashboard> {
        return flow {
            while (true) {
                val zone = ZoneId.systemDefault()
                emit(zone to LocalDate.now(zone))
                delay(DateRefreshIntervalMillis)
            }
        }.distinctUntilChanged().flatMapLatest { (zone, date) ->
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            combine(
                foodDao.observeFoodEntriesForPeriod(start, end),
                foodDao.observeDailyWaterTotal(start),
                profileRepository.observeNutritionGoal()
            ) { entities, waterMilliliters, goal ->
                val entries = entities.map { it.toDomain() }
                DailyNutritionDashboard(
                    goal = goal,
                    total = nutritionCalculator.summarize(entries),
                    waterMilliliters = waterMilliliters,
                    entries = entries
                )
            }
        }
    }

    override fun observeFoodHistory(): Flow<List<FoodLogEntry>> {
        return foodDao.observeFoodEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeFoodFavorites(): Flow<List<FoodFavoritePreset>> {
        return combine(
            foodDao.observeFavoriteProducts(),
            foodDao.observeFavoriteNutrients()
        ) { products, nutrients ->
            products.mapNotNull { product ->
                val productNutrients = nutrients.filter { it.foodProductId == product.id }
                product.toFavoritePreset(productNutrients)
            }
        }
    }

    override fun observeCustomFoods(): Flow<List<CustomFood>> {
        return combine(
            foodDao.observeCustomProducts(),
            foodDao.observeCustomNutrients()
        ) { products, nutrients ->
            products.mapNotNull { product ->
                val productNutrients = nutrients.filter { it.foodProductId == product.id }
                product.toCustomFood(productNutrients)
            }
        }
    }

    override fun observeSavedMeals(): Flow<List<SavedMeal>> {
        return combine(
            foodDao.observeSavedMeals(),
            foodDao.observeSavedMealItems()
        ) { meals, items ->
            meals.map { meal ->
                SavedMeal(
                    id = meal.id,
                    name = meal.name,
                    createdAt = meal.createdAt,
                    items = items
                        .filter { it.savedMealId == meal.id }
                        .sortedBy { it.sortOrder }
                        .map { SavedMealItem(id = it.id, food = it.toFoodLogEntry()) }
                )
            }
        }
    }

    override suspend fun addFood(entry: FoodLogEntry): AppResult<Unit> {
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        database.withTransaction {
            foodDao.upsertFoodEntry(entry.toEntity())
            syncQueueWriter.enqueueOnly(
                entityType = EntityTypeFoodEntry,
                entityId = entry.id,
                operation = OperationUpsert,
                payloadJson = syncPayloadFactory.foodEntry(entry, OperationUpsert)
            )
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "food entry",
            undo = { deleteFood(entry) },
            redo = { addFood(entry) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun addFoods(entries: List<FoodLogEntry>): AppResult<Unit> {
        if (entries.isEmpty()) {
            return AppResult.Failure(AppError.Validation("Add at least one food."))
        }
        entries.firstNotNullOfOrNull(::validate)?.let { error ->
            return AppResult.Failure(error)
        }
        database.withTransaction {
            entries.forEach { entry ->
                foodDao.upsertFoodEntry(entry.toEntity())
                syncQueueWriter.enqueueOnly(
                    entityType = EntityTypeFoodEntry,
                    entityId = entry.id,
                    operation = OperationUpsert,
                    payloadJson = syncPayloadFactory.foodEntry(entry, OperationUpsert)
                )
            }
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "${entries.size} food entries",
            undo = { entries.forEach { deleteFood(it) } },
            redo = { addFoods(entries) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun updateFood(entry: FoodLogEntry): AppResult<Unit> {
        val existing = foodDao.findFoodEntry(entry.id)?.toDomain()
            ?: return AppResult.Failure(AppError.NotFound)
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        val updated = entry.copy(consumedAt = existing.consumedAt)
        database.withTransaction {
            foodDao.upsertFoodEntry(updated.toEntity())
            syncQueueWriter.enqueueOnly(
                entityType = EntityTypeFoodEntry,
                entityId = updated.id,
                operation = OperationUpsert,
                payloadJson = syncPayloadFactory.foodEntry(updated, OperationUpsert)
            )
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "food edit",
            undo = { updateFood(existing) },
            redo = { updateFood(updated) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveFavorite(entry: FoodLogEntry): AppResult<Unit> {
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        if (entry.name.isBlank()) return AppResult.Failure(AppError.Validation("Food name is required."))
        if (entry.grams <= 0.0) return AppResult.Failure(AppError.Validation("Serving size must be greater than zero."))

        val now = System.currentTimeMillis()
        val productId = entry.favoriteProductId()
        foodDao.upsertFoodProduct(
            FoodProductEntity(
                id = productId,
                barcode = null,
                name = entry.name,
                brand = entry.brand,
                servingSizeGrams = entry.grams,
                notes = entry.notes,
                isFavorite = true,
                isCustom = false,
                micronutrientsJson = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        foodDao.upsertFoodNutrients(entry.toFavoriteNutrients(productId, now))
        foodDao.upsertFoodServing(
            FoodServingEntity(
                id = "$productId-serving-default",
                foodProductId = productId,
                label = "Default serving",
                grams = entry.grams,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        recordUndo(
            label = "favorite food",
            undo = {
                deleteFavorite(
                    FoodFavoritePreset(
                        id = productId,
                        name = entry.name,
                        brand = entry.brand,
                        servingSizeGrams = entry.grams,
                        nutritionPer100g = entry.nutrition,
                        notes = entry.notes
                    )
                )
            },
            redo = { saveFavorite(entry) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveCustomFood(entry: FoodLogEntry): AppResult<Unit> {
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        if (entry.name.isBlank()) return AppResult.Failure(AppError.Validation("Food name is required."))
        if (entry.grams <= 0.0) return AppResult.Failure(AppError.Validation("Serving size must be greater than zero."))

        val now = System.currentTimeMillis()
        val productId = entry.customFoodId()
        foodDao.upsertFoodProduct(
            FoodProductEntity(
                id = productId,
                barcode = null,
                name = entry.name,
                brand = entry.brand,
                servingSizeGrams = entry.grams,
                notes = entry.notes,
                isFavorite = false,
                isCustom = true,
                micronutrientsJson = com.raysix.fitns.core.serialization.MicronutrientsCodec.encode(entry.micronutrients),
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        foodDao.upsertFoodNutrients(entry.toFavoriteNutrients(productId, now))
        foodDao.upsertFoodServing(
            FoodServingEntity(
                id = "$productId-serving-default",
                foodProductId = productId,
                label = "Default serving",
                grams = entry.grams,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        recordUndo(
            label = "custom food",
            undo = {
                deleteCustomFood(
                    CustomFood(
                        id = productId,
                        name = entry.name,
                        brand = entry.brand,
                        servingSizeGrams = entry.grams,
                        nutritionPer100g = entry.nutrition,
                        notes = entry.notes,
                        micronutrients = entry.micronutrients
                    )
                )
            },
            redo = { saveCustomFood(entry) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteCustomFood(customFood: CustomFood): AppResult<Unit> {
        foodDao.softDeleteFoodProduct(customFood.id, System.currentTimeMillis())
        recordUndo(
            label = "custom food delete",
            undo = {
                saveCustomFood(
                    FoodLogEntry(
                        name = customFood.name,
                        brand = customFood.brand,
                        mealType = MealType.Custom,
                        grams = customFood.servingSizeGrams,
                        nutrition = customFood.nutritionPer100g,
                        notes = customFood.notes,
                        micronutrients = customFood.micronutrients
                    )
                )
            },
            redo = { deleteCustomFood(customFood) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveMeal(meal: SavedMeal): AppResult<Unit> {
        if (meal.name.isBlank()) return AppResult.Failure(AppError.Validation("Meal name is required."))
        if (meal.items.isEmpty()) return AppResult.Failure(AppError.Validation("Save at least one food in a meal."))
        meal.items.firstNotNullOfOrNull { validate(it.food) }
            ?.let { return AppResult.Failure(it) }
        val now = System.currentTimeMillis()
        foodDao.upsertSavedMealWithItems(
            meal = SavedMealEntity(
                id = meal.id,
                name = meal.name.trim(),
                createdAt = meal.createdAt,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            ),
            items = meal.items.mapIndexed { index, item ->
                item.toEntity(savedMealId = meal.id, sortOrder = index, now = now)
            }
        )
        recordUndo(
            label = "saved meal",
            undo = { deleteSavedMeal(meal) },
            redo = { saveMeal(meal) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteSavedMeal(meal: SavedMeal): AppResult<Unit> {
        foodDao.softDeleteSavedMealCascade(meal.id, System.currentTimeMillis())
        recordUndo(
            label = "saved meal delete",
            undo = { saveMeal(meal) },
            redo = { deleteSavedMeal(meal) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun logSavedMeal(meal: SavedMeal, scaleFactor: Double, mealType: MealType): AppResult<Unit> {
        if (!scaleFactor.isFinite() || scaleFactor <= 0.0) {
            return AppResult.Failure(AppError.Validation("Meal scale must be a finite number greater than zero."))
        }
        return copyEntries(
            entries = mealScaler.scale(meal.items.map { it.food }, scaleFactor)
                .map { it.copy(mealType = mealType) },
            mealType = null
        )
    }

    override suspend fun copyEntries(entries: List<FoodLogEntry>, mealType: MealType?): AppResult<Unit> {
        if (entries.isEmpty()) return AppResult.Failure(AppError.Validation("No foods are available to copy."))
        val now = System.currentTimeMillis()
        val copies = entries.map { entry ->
            entry.copy(id = UUID.randomUUID().toString(), mealType = mealType ?: entry.mealType, consumedAt = now)
        }
        copies.firstNotNullOfOrNull(::validate)?.let { return AppResult.Failure(it) }
        database.withTransaction {
            copies.forEach { copied ->
                foodDao.upsertFoodEntry(copied.toEntity(now))
                syncQueueWriter.enqueueOnly(
                    entityType = EntityTypeFoodEntry,
                    entityId = copied.id,
                    operation = OperationUpsert,
                    payloadJson = syncPayloadFactory.foodEntry(copied, OperationUpsert)
                )
            }
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "${copies.size} copied food entries",
            undo = { copies.forEach { deleteFood(it) } },
            redo = { addFoods(copies) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteFavorite(favorite: FoodFavoritePreset): AppResult<Unit> {
        foodDao.softDeleteFoodProduct(favorite.id, System.currentTimeMillis())
        recordUndo(
            label = "favorite delete",
            undo = {
                saveFavorite(
                    FoodLogEntry(
                        name = favorite.name,
                        brand = favorite.brand,
                        mealType = MealType.Custom,
                        grams = favorite.servingSizeGrams,
                        nutrition = favorite.nutritionPer100g,
                        notes = favorite.notes
                    )
                )
            },
            redo = { deleteFavorite(favorite) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun addWater(milliliters: Double): AppResult<Unit> {
        if (!milliliters.isFinite() || milliliters <= 0.0) {
            return AppResult.Failure(AppError.Validation("Water amount must be a finite number greater than zero."))
        }
        if (milliliters > 3000.0) return AppResult.Failure(AppError.Validation("Water amount looks implausibly high."))

        val zone = ZoneId.systemDefault()
        val dayStartMillis = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        foodDao.addWaterAtomically(
            dayStartMillis = dayStartMillis,
            milliliters = milliliters,
            updatedAt = now,
            summaryIfMissing = DailyNutritionSummaryEntity(
                id = UUID.randomUUID().toString(),
                dayStartMillis = dayStartMillis,
                caloriesKcal = 0.0,
                proteinGrams = 0.0,
                carbohydratesGrams = 0.0,
                fatGrams = 0.0,
                fiberGrams = 0.0,
                waterMilliliters = milliliters,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        recordUndo(
            label = "water adjustment",
            undo = { removeWater(milliliters) },
            redo = { addWater(milliliters) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun removeWater(milliliters: Double): AppResult<Unit> {
        if (!milliliters.isFinite() || milliliters <= 0.0) {
            return AppResult.Failure(AppError.Validation("Water amount must be a finite number greater than zero."))
        }
        val zone = ZoneId.systemDefault()
        val dayStartMillis = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        foodDao.removeWaterAtomically(
            dayStartMillis = dayStartMillis,
            milliliters = milliliters,
            updatedAt = System.currentTimeMillis()
        )
        recordUndo(
            label = "water adjustment",
            undo = { addWater(milliliters) },
            redo = { removeWater(milliliters) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteFood(entry: FoodLogEntry): AppResult<Unit> {
        val existing = foodDao.findFoodEntry(entry.id)?.toDomain()
            ?: return AppResult.Failure(AppError.NotFound)
        database.withTransaction {
            foodDao.softDeleteFoodEntry(entry.id, System.currentTimeMillis())
            syncQueueWriter.enqueueOnly(
                entityType = EntityTypeFoodEntry,
                entityId = entry.id,
                operation = OperationDelete,
                payloadJson = syncPayloadFactory.foodEntry(existing, OperationDelete)
            )
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "food delete",
            undo = { addFood(existing) },
            redo = { deleteFood(existing) }
        )
        return AppResult.Success(Unit)
    }

    private fun recordUndo(label: String, undo: suspend () -> Unit, redo: suspend () -> Unit) {
        undoRedoManager.record(UndoRedoAction(label = label, undo = undo, redo = redo))
    }

    private fun validate(entry: FoodLogEntry): AppError? {
        return when {
            entry.name.isBlank() -> AppError.Validation("Food name is required.")
            !entry.grams.isFinite() || entry.grams <= 0.0 -> AppError.Validation("Amount in grams must be a finite number greater than zero.")
            !entry.nutrition.caloriesKcal.isFinite() || entry.nutrition.caloriesKcal < 0.0 -> AppError.Validation("Calories must be finite and non-negative.")
            !entry.nutrition.proteinGrams.isFinite() || entry.nutrition.proteinGrams < 0.0 -> AppError.Validation("Protein must be finite and non-negative.")
            !entry.nutrition.carbohydratesGrams.isFinite() || entry.nutrition.carbohydratesGrams < 0.0 -> AppError.Validation("Carbs must be finite and non-negative.")
            !entry.nutrition.sugarGrams.isFinite() || entry.nutrition.sugarGrams < 0.0 -> AppError.Validation("Sugar must be finite and non-negative.")
            !entry.nutrition.fatGrams.isFinite() || entry.nutrition.fatGrams < 0.0 -> AppError.Validation("Fat must be finite and non-negative.")
            !entry.nutrition.saturatedFatGrams.isFinite() || entry.nutrition.saturatedFatGrams < 0.0 -> AppError.Validation("Saturated fat must be finite and non-negative.")
            !entry.nutrition.fiberGrams.isFinite() || entry.nutrition.fiberGrams < 0.0 -> AppError.Validation("Fiber must be finite and non-negative.")
            !entry.nutrition.saltGrams.isFinite() || entry.nutrition.saltGrams < 0.0 -> AppError.Validation("Salt must be finite and non-negative.")
            entry.nutrition.sodiumMilligrams?.let { !it.isFinite() || it < 0.0 } == true -> AppError.Validation("Sodium must be finite and non-negative.")
            entry.micronutrients.values.values.any { !it.amount.isFinite() || it.amount < 0.0 } ->
                AppError.Validation("Micronutrient amounts must be finite and non-negative.")
            else -> null
        }
    }

    private fun FoodProductEntity.toFavoritePreset(nutrients: List<FoodNutrientEntity>): FoodFavoritePreset? {
        if (name.isBlank()) return null
        return FoodFavoritePreset(
            id = id,
            name = name,
            brand = brand,
            servingSizeGrams = servingSizeGrams ?: 100.0,
            notes = notes,
            nutritionPer100g = NutritionFacts(
                caloriesKcal = nutrients.amountFor(NutrientCalories),
                proteinGrams = nutrients.amountFor(NutrientProtein),
                carbohydratesGrams = nutrients.amountFor(NutrientCarbs),
                sugarGrams = nutrients.amountFor(NutrientSugar),
                fatGrams = nutrients.amountFor(NutrientFat),
                saturatedFatGrams = nutrients.amountFor(NutrientSaturatedFat),
                fiberGrams = nutrients.amountFor(NutrientFiber),
                saltGrams = nutrients.amountFor(NutrientSalt),
                sodiumMilligrams = nutrients.amountFor(NutrientSodium).takeIf { it > 0.0 }
            )
        )
    }

    private fun FoodProductEntity.toCustomFood(nutrients: List<FoodNutrientEntity>): CustomFood? {
        if (name.isBlank()) return null
        return CustomFood(
            id = id,
            name = name,
            brand = brand,
            servingSizeGrams = servingSizeGrams ?: 100.0,
            notes = notes,
            micronutrients = com.raysix.fitns.core.serialization.MicronutrientsCodec.decode(micronutrientsJson),
            nutritionPer100g = NutritionFacts(
                caloriesKcal = nutrients.amountFor(NutrientCalories),
                proteinGrams = nutrients.amountFor(NutrientProtein),
                carbohydratesGrams = nutrients.amountFor(NutrientCarbs),
                sugarGrams = nutrients.amountFor(NutrientSugar),
                fatGrams = nutrients.amountFor(NutrientFat),
                saturatedFatGrams = nutrients.amountFor(NutrientSaturatedFat),
                fiberGrams = nutrients.amountFor(NutrientFiber),
                saltGrams = nutrients.amountFor(NutrientSalt),
                sodiumMilligrams = nutrients.amountFor(NutrientSodium).takeIf { it > 0.0 }
            )
        )
    }

    private fun List<FoodNutrientEntity>.amountFor(nutrientId: String): Double {
        return firstOrNull { it.nutrientId == nutrientId }?.amountPer100g ?: 0.0
    }

    private fun FoodLogEntry.toFavoriteNutrients(productId: String, now: Long): List<FoodNutrientEntity> {
        val factor = if (grams <= 0.0) 0.0 else 100.0 / grams
        return listOf(
            NutrientCalories to nutrition.caloriesKcal * factor,
            NutrientProtein to nutrition.proteinGrams * factor,
            NutrientCarbs to nutrition.carbohydratesGrams * factor,
            NutrientSugar to nutrition.sugarGrams * factor,
            NutrientFat to nutrition.fatGrams * factor,
            NutrientSaturatedFat to nutrition.saturatedFatGrams * factor,
            NutrientFiber to nutrition.fiberGrams * factor,
            NutrientSalt to nutrition.saltGrams * factor,
            NutrientSodium to ((nutrition.sodiumMilligrams ?: 0.0) * factor)
        ).map { (nutrientId, amount) ->
            FoodNutrientEntity(
                id = "$productId-$nutrientId",
                foodProductId = productId,
                nutrientId = nutrientId,
                amountPer100g = amount,
                dataQuality = dataQuality.name,
                source = "favorite"
            )
        }
    }

    private fun FoodLogEntry.favoriteProductId(): String {
        val normalized = "${name.trim().lowercase()}|${brand.orEmpty().trim().lowercase()}"
        val uuid = UUID.nameUUIDFromBytes(normalized.toByteArray(StandardCharsets.UTF_8))
        return "favorite-$uuid"
    }

    private fun FoodLogEntry.customFoodId(): String {
        val normalized = "custom|${name.trim().lowercase()}|${brand.orEmpty().trim().lowercase()}"
        val uuid = UUID.nameUUIDFromBytes(normalized.toByteArray(StandardCharsets.UTF_8))
        return "custom-$uuid"
    }

    private fun SavedMealItem.toEntity(savedMealId: String, sortOrder: Int, now: Long): SavedMealItemEntity {
        return SavedMealItemEntity(
            id = id,
            savedMealId = savedMealId,
            foodEntrySnapshotId = food.id,
            name = food.name,
            brand = food.brand,
            mealType = food.mealType.name,
            grams = food.grams,
            caloriesKcal = food.nutrition.caloriesKcal,
            proteinGrams = food.nutrition.proteinGrams,
            carbohydratesGrams = food.nutrition.carbohydratesGrams,
            sugarGrams = food.nutrition.sugarGrams,
            fatGrams = food.nutrition.fatGrams,
            saturatedFatGrams = food.nutrition.saturatedFatGrams,
            fiberGrams = food.nutrition.fiberGrams,
            saltGrams = food.nutrition.saltGrams,
            sodiumMilligrams = food.nutrition.sodiumMilligrams,
            micronutrientsJson = com.raysix.fitns.core.serialization.MicronutrientsCodec.encode(food.micronutrients),
            notes = food.notes,
            dataQuality = food.dataQuality.name,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = SyncStatus.PendingSync,
            serverVersion = null
        )
    }

    private fun SavedMealItemEntity.toFoodLogEntry(): FoodLogEntry {
        return FoodLogEntry(
            id = foodEntrySnapshotId,
            name = name,
            brand = brand,
            mealType = MealType.entries.firstOrNull { it.name == mealType } ?: MealType.Custom,
            grams = grams,
            nutrition = NutritionFacts(
                caloriesKcal = caloriesKcal,
                proteinGrams = proteinGrams,
                carbohydratesGrams = carbohydratesGrams,
                sugarGrams = sugarGrams,
                fatGrams = fatGrams,
                saturatedFatGrams = saturatedFatGrams,
                fiberGrams = fiberGrams,
                saltGrams = saltGrams,
                sodiumMilligrams = sodiumMilligrams
            ),
            dataQuality = com.raysix.fitns.domain.model.DataQuality.entries.firstOrNull { it.name == dataQuality }
                ?: com.raysix.fitns.domain.model.DataQuality.Missing,
            notes = notes,
            consumedAt = createdAt,
            micronutrients = com.raysix.fitns.core.serialization.MicronutrientsCodec.decode(micronutrientsJson)
        )
    }

    private companion object {
        const val EntityTypeFoodEntry = "FoodEntry"
        const val OperationUpsert = "upsert"
        const val OperationDelete = "delete"
        const val NutrientCalories = "calories_kcal"
        const val NutrientProtein = "protein_g"
        const val NutrientCarbs = "carbs_g"
        const val NutrientSugar = "sugar_g"
        const val NutrientFat = "fat_g"
        const val NutrientSaturatedFat = "saturated_fat_g"
        const val NutrientFiber = "fiber_g"
        const val NutrientSalt = "salt_g"
        const val NutrientSodium = "sodium_mg"
    }
}
