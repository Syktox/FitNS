package com.raysix.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.FoodEntryEntity
import com.raysix.fitns.data.local.entity.FoodNutrientEntity
import com.raysix.fitns.data.local.entity.FoodProductEntity
import com.raysix.fitns.data.local.entity.FoodServingEntity
import com.raysix.fitns.data.local.entity.SavedMealEntity
import com.raysix.fitns.data.local.entity.SavedMealItemEntity
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

    @Query("SELECT * FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL LIMIT 1")
    fun observeDailySummary(dayStartMillis: Long): Flow<DailyNutritionSummaryEntity?>

    @Query("SELECT * FROM daily_nutrition_summaries WHERE dayStartMillis = :dayStartMillis AND deletedAt IS NULL LIMIT 1")
    suspend fun findDailySummary(dayStartMillis: Long): DailyNutritionSummaryEntity?

    @Query("SELECT * FROM food_entries WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun findFoodEntry(id: String): FoodEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodEntry(entry: FoodEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailyNutritionSummaryEntity)

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
