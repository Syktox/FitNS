package com.syktox.fitns.domain.repository

import com.syktox.fitns.core.model.AppResult
import com.syktox.fitns.core.settings.N8nConnectionSettings
import com.syktox.fitns.domain.model.BodyWeightLogEntry
import com.syktox.fitns.domain.model.DailyNutritionDashboard
import com.syktox.fitns.domain.model.Exercise
import com.syktox.fitns.domain.model.FoodLogEntry
import com.syktox.fitns.domain.model.FoodFavoritePreset
import com.syktox.fitns.domain.model.FoodProductLookup
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.model.UserProfile
import com.syktox.fitns.domain.model.WorkoutLogEntry
import com.syktox.fitns.domain.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun observeToday(): Flow<DailyNutritionDashboard>
    fun observeFoodHistory(): Flow<List<FoodLogEntry>>
    fun observeFoodFavorites(): Flow<List<FoodFavoritePreset>>
    suspend fun addFood(entry: FoodLogEntry): AppResult<Unit>
    suspend fun saveFavorite(entry: FoodLogEntry): AppResult<Unit>
    suspend fun deleteFavorite(favorite: FoodFavoritePreset): AppResult<Unit>
    suspend fun addWater(milliliters: Double): AppResult<Unit>
    suspend fun deleteFood(entry: FoodLogEntry): AppResult<Unit>
}

interface WorkoutRepository {
    fun observeExercises(): Flow<List<Exercise>>
    fun observeHistory(): Flow<List<WorkoutLogEntry>>
    fun observeWorkoutPlans(): Flow<List<WorkoutPlan>>
    suspend fun addExercise(exercise: Exercise): AppResult<Unit>
    suspend fun addWorkout(entry: WorkoutLogEntry): AppResult<Unit>
    suspend fun saveWorkoutPlan(plan: WorkoutPlan): AppResult<Unit>
    suspend fun deleteWorkoutPlan(plan: WorkoutPlan): AppResult<Unit>
    suspend fun deleteWorkout(entry: WorkoutLogEntry): AppResult<Unit>
}

interface BodyWeightRepository {
    fun observeHistory(): Flow<List<BodyWeightLogEntry>>
    suspend fun addEntry(entry: BodyWeightLogEntry): AppResult<Unit>
    suspend fun deleteEntry(entry: BodyWeightLogEntry): AppResult<Unit>
}

interface ProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeNutritionGoal(): Flow<NutritionGoal>
    suspend fun saveProfile(profile: UserProfile): AppResult<Unit>
    suspend fun saveNutritionGoal(goal: NutritionGoal): AppResult<Unit>
}

interface N8nRepository {
    suspend fun testConnection(baseUrl: String, bearerToken: String?): AppResult<Unit>
    suspend fun findProductByBarcode(baseUrl: String, bearerToken: String?, barcode: String): AppResult<FoodProductLookup>
}

interface SettingsRepository {
    fun observeN8nSettings(): Flow<N8nConnectionSettings>
    fun observeTemporaryPhotosOnly(): Flow<Boolean>
    suspend fun updateN8nBaseUrl(baseUrl: String)
    suspend fun updateSyncEnabled(enabled: Boolean)
    suspend fun updateTemporaryPhotosOnly(enabled: Boolean)
}
