package com.syktox.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.syktox.fitns.data.local.entity.NutritionGoalEntity
import com.syktox.fitns.data.local.entity.UserProfileEntity
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionGoal(goal: NutritionGoalEntity)
}

