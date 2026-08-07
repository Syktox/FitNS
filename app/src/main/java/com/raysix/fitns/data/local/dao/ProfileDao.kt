package com.raysix.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raysix.fitns.data.local.entity.NutrientTargetEntity
import com.raysix.fitns.data.local.entity.NutritionGoalEntity
import com.raysix.fitns.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    fun observeProfile(id: String): Flow<UserProfileEntity?>

    @Query(
        """
        SELECT * FROM nutrition_goals
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL
        ORDER BY validFrom DESC
        LIMIT 1
        """
    )
    fun observeLatestNutritionGoal(userProfileId: String): Flow<NutritionGoalEntity?>

    @Query(
        """
        SELECT * FROM nutrition_goals
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL AND validFrom <= :dateMillis
        ORDER BY validFrom DESC
        LIMIT 1
        """
    )
    fun observeNutritionGoalForDate(userProfileId: String, dateMillis: Long): Flow<NutritionGoalEntity?>

    @Query(
        """
        SELECT * FROM nutrition_goals
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL
        ORDER BY validFrom DESC
        """
    )
    fun observeNutritionGoalHistory(userProfileId: String): Flow<List<NutritionGoalEntity>>

    @Query(
        """
        SELECT * FROM nutrient_targets
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL AND validTo IS NULL
        ORDER BY nutrientKey ASC
        """
    )
    fun observeActiveNutrientTargets(userProfileId: String): Flow<List<NutrientTargetEntity>>

    @Query(
        """
        SELECT * FROM nutrient_targets
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL AND validFrom <= :dateMillis
        ORDER BY validFrom DESC
        """
    )
    fun observeNutrientTargetsForDate(userProfileId: String, dateMillis: Long): Flow<List<NutrientTargetEntity>>

    @Query("SELECT * FROM nutrition_goals WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun findNutritionGoal(id: String): NutritionGoalEntity?

    @Query(
        """
        SELECT * FROM nutrition_goals
        WHERE userProfileId = :userProfileId AND deletedAt IS NULL AND validTo IS NULL
        ORDER BY validFrom DESC
        LIMIT 1
        """
    )
    suspend fun findOpenNutritionGoal(userProfileId: String): NutritionGoalEntity?

    @Query("UPDATE nutrition_goals SET validTo = :validTo, updatedAt = :updatedAt WHERE id = :id")
    suspend fun closeNutritionGoal(id: String, validTo: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionGoal(goal: NutritionGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutrientTargets(targets: List<NutrientTargetEntity>)

    @Query("UPDATE nutrient_targets SET validTo = :validTo, updatedAt = :updatedAt WHERE userProfileId = :userProfileId AND validTo IS NULL")
    suspend fun closeAllNutrientTargets(userProfileId: String, validTo: Long, updatedAt: Long)
}
