package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncPayloadFactory
import com.raysix.fitns.core.sync.SyncQueueWriter
import com.raysix.fitns.data.local.dao.FoodDao
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.FoodNutrientEntity
import com.raysix.fitns.data.local.entity.FoodProductEntity
import com.raysix.fitns.data.local.entity.FoodServingEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.FoodFavoritePreset
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.usecase.NutritionCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

class LocalNutritionRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val nutritionCalculator: NutritionCalculator,
    private val syncQueueWriter: SyncQueueWriter,
    private val syncPayloadFactory: SyncPayloadFactory,
    private val profileRepository: ProfileRepository
) : NutritionRepository {
    override fun observeToday(): Flow<DailyNutritionDashboard> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return combine(
            foodDao.observeFoodEntriesForPeriod(start, end),
            foodDao.observeDailySummary(start),
            profileRepository.observeNutritionGoal()
        ) { entities, summary, goal ->
            val entries = entities.map { it.toDomain() }
            DailyNutritionDashboard(
                goal = goal,
                total = nutritionCalculator.summarize(entries),
                waterMilliliters = summary?.waterMilliliters ?: 0.0,
                entries = entries
            )
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

    override suspend fun addFood(entry: FoodLogEntry): AppResult<Unit> {
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        foodDao.upsertFoodEntry(entry.toEntity())
        syncQueueWriter.enqueue(
            entityType = EntityTypeFoodEntry,
            entityId = entry.id,
            operation = OperationUpsert,
            payloadJson = syncPayloadFactory.foodEntry(entry, OperationUpsert)
        )
        return AppResult.Success(Unit)
    }

    override suspend fun updateFood(entry: FoodLogEntry): AppResult<Unit> {
        val existing = foodDao.findFoodEntry(entry.id)
            ?: return AppResult.Failure(AppError.NotFound)
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        val updated = entry.copy(consumedAt = existing.consumedAt)
        foodDao.upsertFoodEntry(updated.toEntity())
        syncQueueWriter.enqueue(
            entityType = EntityTypeFoodEntry,
            entityId = updated.id,
            operation = OperationUpsert,
            payloadJson = syncPayloadFactory.foodEntry(updated, OperationUpsert)
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
        return AppResult.Success(Unit)
    }

    override suspend fun deleteFavorite(favorite: FoodFavoritePreset): AppResult<Unit> {
        foodDao.softDeleteFoodProduct(favorite.id, System.currentTimeMillis())
        return AppResult.Success(Unit)
    }

    override suspend fun addWater(milliliters: Double): AppResult<Unit> {
        if (milliliters <= 0.0) return AppResult.Failure(AppError.Validation("Water amount must be greater than zero."))
        if (milliliters > 3000.0) return AppResult.Failure(AppError.Validation("Water amount looks implausibly high."))

        val zone = ZoneId.systemDefault()
        val dayStartMillis = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val existing = foodDao.findDailySummary(dayStartMillis)
        val updated = existing?.copy(
            waterMilliliters = existing.waterMilliliters + milliliters,
            updatedAt = now,
            syncStatus = SyncStatus.PendingSync
        ) ?: DailyNutritionSummaryEntity(
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
        foodDao.upsertDailySummary(updated)
        return AppResult.Success(Unit)
    }

    override suspend fun deleteFood(entry: FoodLogEntry): AppResult<Unit> {
        val existing = foodDao.findFoodEntry(entry.id)?.toDomain()
            ?: return AppResult.Failure(AppError.NotFound)
        foodDao.softDeleteFoodEntry(entry.id, System.currentTimeMillis())
        syncQueueWriter.enqueue(
            entityType = EntityTypeFoodEntry,
            entityId = entry.id,
            operation = OperationDelete,
            payloadJson = syncPayloadFactory.foodEntry(existing, OperationDelete)
        )
        return AppResult.Success(Unit)
    }

    private fun validate(entry: FoodLogEntry): AppError? {
        return when {
            entry.grams < 0.0 -> AppError.Validation("Amount in grams cannot be negative.")
            entry.nutrition.caloriesKcal < 0.0 -> AppError.Validation("Calories cannot be negative.")
            entry.nutrition.proteinGrams < 0.0 -> AppError.Validation("Protein cannot be negative.")
            entry.nutrition.carbohydratesGrams < 0.0 -> AppError.Validation("Carbs cannot be negative.")
            entry.nutrition.sugarGrams < 0.0 -> AppError.Validation("Sugar cannot be negative.")
            entry.nutrition.fatGrams < 0.0 -> AppError.Validation("Fat cannot be negative.")
            entry.nutrition.saturatedFatGrams < 0.0 -> AppError.Validation("Saturated fat cannot be negative.")
            entry.nutrition.fiberGrams < 0.0 -> AppError.Validation("Fiber cannot be negative.")
            entry.nutrition.saltGrams < 0.0 -> AppError.Validation("Salt cannot be negative.")
            (entry.nutrition.sodiumMilligrams ?: 0.0) < 0.0 -> AppError.Validation("Sodium cannot be negative.")
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
