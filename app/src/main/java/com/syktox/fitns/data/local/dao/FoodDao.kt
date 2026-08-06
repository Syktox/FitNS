package com.syktox.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.syktox.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.syktox.fitns.data.local.entity.FoodEntryEntity
import com.syktox.fitns.data.local.entity.FoodNutrientEntity
import com.syktox.fitns.data.local.entity.FoodProductEntity
import com.syktox.fitns.data.local.entity.FoodServingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE deletedAt IS NULL AND consumedAt >= :fromMillis AND consumedAt < :toMillis ORDER BY consumedAt DESC")
    fun observeFoodEntriesForPeriod(fromMillis: Long, toMillis: Long): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE deletedAt IS NULL ORDER BY consumedAt DESC")
    fun observeFoodEntries(): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_products WHERE isFavorite = 1 AND deletedAt IS NULL ORDER BY name ASC")
    fun observeFavoriteProducts(): Flow<List<FoodProductEntity>>

    @Query(
        """
        SELECT food_nutrients.* FROM food_nutrients
        INNER JOIN food_products ON food_products.id = food_nutrients.foodProductId
        WHERE food_products.isFavorite = 1 AND food_products.deletedAt IS NULL
        """
    )
    fun observeFavoriteNutrients(): Flow<List<FoodNutrientEntity>>

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

    @Query("UPDATE food_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDeleteFoodEntry(id: String, deletedAt: Long)

    @Query("UPDATE food_products SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDeleteFoodProduct(id: String, deletedAt: Long)
}
