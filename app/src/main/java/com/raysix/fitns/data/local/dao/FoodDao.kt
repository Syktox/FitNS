package com.raysix.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.FoodEntryEntity
import com.raysix.fitns.data.local.entity.FoodNutrientEntity
import com.raysix.fitns.data.local.entity.FoodProductEntity
import com.raysix.fitns.data.local.entity.FoodServingEntity
import com.raysix.fitns.data.local.entity.SavedMealEntity
import com.raysix.fitns.data.local.entity.SavedMealItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE deletedAt IS NULL AND consumedAt >= :fromMillis AND consumedAt < :toMillis ORDER BY consumedAt DESC")
    fun observeFoodEntriesForPeriod(fromMillis: Long, toMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE deletedAt IS NULL ORDER BY consumedAt DESC")
    fun observeFoodEntries(): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_products WHERE isFavorite = 1 AND deletedAt IS NULL ORDER BY name ASC")
    fun observeFavoriteProducts(): Flow<List<FoodProductEntity>>

    @Query("SELECT * FROM food_products WHERE isCustom = 1 AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeCustomProducts(): Flow<List<FoodProductEntity>>

    @Query(
        """
        SELECT food_nutrients.* FROM food_nutrients
        INNER JOIN food_products ON food_products.id = food_nutrients.foodProductId
        WHERE food_products.isFavorite = 1 AND food_products.deletedAt IS NULL
        """
    )
    fun observeFavoriteNutrients(): Flow<List<FoodNutrientEntity>>

    @Query(
        """
        SELECT food_nutrients.* FROM food_nutrients
        INNER JOIN food_products ON food_products.id = food_nutrients.foodProductId
        WHERE food_products.isCustom = 1 AND food_products.deletedAt IS NULL
        """
    )
    fun observeCustomNutrients(): Flow<List<FoodNutrientEntity>>

    @Query("SELECT * FROM saved_meals WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeSavedMeals(): Flow<List<SavedMealEntity>>

    @Query("SELECT * FROM saved_meal_items WHERE deletedAt IS NULL ORDER BY savedMealId ASC, sortOrder ASC")
    fun observeSavedMealItems(): Flow<List<SavedMealItemEntity>>

    @Query("SELECT * FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL ORDER BY createdAt ASC, id ASC LIMIT 1")
    fun observeDailySummary(dayStartMillis: Long): Flow<DailyNutritionSummaryEntity?>

    @Query("SELECT COALESCE(SUM(waterMilliliters), 0.0) FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL")
    fun observeDailyWaterTotal(dayStartMillis: Long): Flow<Double>

    @Query("SELECT * FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL ORDER BY createdAt ASC, id ASC LIMIT 1")
    suspend fun findDailySummary(dayStartMillis: Long): DailyNutritionSummaryEntity?

    @Query("SELECT * FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL ORDER BY createdAt ASC, id ASC")
    suspend fun findDailySummaries(dayStartMillis: Long): List<DailyNutritionSummaryEntity>

    @Query("SELECT * FROM food_entries WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun findFoodEntry(id: String): FoodEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodEntry(entry: FoodEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailyNutritionSummaryEntity)

    @Query("DELETE FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL AND id != :canonicalId")
    suspend fun deleteDuplicateDailySummaries(dayStartMillis: Long, canonicalId: String)

    @Query(
        """
        UPDATE daily_nutrition_summaries
        SET waterMilliliters = waterMilliliters + :milliliters,
            updatedAt = :updatedAt,
            syncStatus = 'PendingSync'
        WHERE id = (
            SELECT id FROM daily_nutrition_summaries
            WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL
            ORDER BY createdAt ASC
            LIMIT 1
        )
        """
    )
    suspend fun incrementWater(dayStartMillis: Long, milliliters: Double, updatedAt: Long): Int

    @Query(
        """
        UPDATE daily_nutrition_summaries
        SET waterMilliliters = MAX(waterMilliliters - :milliliters, 0.0),
            updatedAt = :updatedAt,
            syncStatus = 'PendingSync'
        WHERE id = (
            SELECT id FROM daily_nutrition_summaries
            WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL
            ORDER BY createdAt ASC
            LIMIT 1
        )
        """
    )
    suspend fun decrementWater(dayStartMillis: Long, milliliters: Double, updatedAt: Long)

    @Transaction
    suspend fun addWaterAtomically(
        dayStartMillis: Long,
        milliliters: Double,
        updatedAt: Long,
        summaryIfMissing: DailyNutritionSummaryEntity
    ) {
        consolidateDailySummaries(dayStartMillis, updatedAt)
        if (incrementWater(dayStartMillis, milliliters, updatedAt) == 0) {
            upsertDailySummary(
                summaryIfMissing.copy(
                    dayStartMillis = dayStartMillis,
                    waterMilliliters = milliliters,
                    updatedAt = updatedAt
                )
            )
        }
    }

    @Transaction
    suspend fun removeWaterAtomically(dayStartMillis: Long, milliliters: Double, updatedAt: Long) {
        consolidateDailySummaries(dayStartMillis, updatedAt)
        decrementWater(dayStartMillis, milliliters, updatedAt)
    }

    @Transaction
    suspend fun consolidateDailySummaries(
        dayStartMillis: Long,
        updatedAt: Long
    ): DailyNutritionSummaryEntity? {
        val summaries = findDailySummaries(dayStartMillis)
        if (summaries.size <= 1) return summaries.firstOrNull()

        fun finiteTotal(selector: (DailyNutritionSummaryEntity) -> Double): Double {
            return summaries.sumOf { summary ->
                selector(summary).takeIf { it.isFinite() } ?: 0.0
            }
        }

        val canonical = summaries.first().copy(
            caloriesKcal = finiteTotal(DailyNutritionSummaryEntity::caloriesKcal),
            proteinGrams = finiteTotal(DailyNutritionSummaryEntity::proteinGrams),
            carbohydratesGrams = finiteTotal(DailyNutritionSummaryEntity::carbohydratesGrams),
            fatGrams = finiteTotal(DailyNutritionSummaryEntity::fatGrams),
            fiberGrams = finiteTotal(DailyNutritionSummaryEntity::fiberGrams),
            waterMilliliters = finiteTotal(DailyNutritionSummaryEntity::waterMilliliters).coerceAtLeast(0.0),
            updatedAt = maxOf(updatedAt, summaries.maxOf(DailyNutritionSummaryEntity::updatedAt)),
            syncStatus = SyncStatus.PendingSync,
            serverVersion = null
        )
        upsertDailySummary(canonical)
        deleteDuplicateDailySummaries(dayStartMillis, canonical.id)
        return canonical
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodProduct(product: FoodProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodNutrients(nutrients: List<FoodNutrientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodServing(serving: FoodServingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMeal(meal: SavedMealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMealItems(items: List<SavedMealItemEntity>)

    @Query("UPDATE saved_meals SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDeleteSavedMeal(id: String, deletedAt: Long)

    @Query("UPDATE saved_meal_items SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE savedMealId = :savedMealId")
    suspend fun softDeleteSavedMealItems(savedMealId: String, deletedAt: Long)

    @Query("UPDATE food_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDeleteFoodEntry(id: String, deletedAt: Long)

    @Query("UPDATE food_products SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDeleteFoodProduct(id: String, deletedAt: Long)

    @androidx.room.Transaction
    suspend fun upsertSavedMealWithItems(meal: SavedMealEntity, items: List<SavedMealItemEntity>) {
        val now = System.currentTimeMillis()
        softDeleteSavedMealItems(meal.id, now)
        upsertSavedMeal(meal)
        upsertSavedMealItems(items)
    }

    @androidx.room.Transaction
    suspend fun softDeleteSavedMealCascade(id: String, deletedAt: Long) {
        softDeleteSavedMeal(id, deletedAt)
        softDeleteSavedMealItems(id, deletedAt)
    }
}
