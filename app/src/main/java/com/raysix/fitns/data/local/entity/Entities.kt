package com.raysix.fitns.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SyncStatus {
    LocalOnly,
    PendingSync,
    Synced,
    Conflict,
    Failed
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val age: Int?,
    val sexOrPhysiology: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val targetWeightKg: Double?,
    val activityLevel: String,
    val trainingDaysPerWeek: Int,
    val goal: String,
    val dietStyle: String?,
    val allergies: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(
    tableName = "nutrition_goals",
    indices = [Index(value = ["userProfileId", "validFrom"])]
)
data class NutritionGoalEntity(
    @PrimaryKey val id: String,
    val userProfileId: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double,
    val waterMilliliters: Double,
    val validFrom: Long,
    val validTo: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(
    tableName = "nutrient_targets",
    indices = [Index(value = ["userProfileId"])]
)
data class NutrientTargetEntity(
    @PrimaryKey val id: String,
    val userProfileId: String,
    val nutrientKey: String,
    val targetAmount: Double,
    val unit: String,
    val source: String,
    val validFrom: Long,
    val validTo: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "food_products")
data class FoodProductEntity(
    @PrimaryKey val id: String,
    val barcode: String?,
    val name: String,
    val brand: String?,
    val servingSizeGrams: Double?,
    val notes: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "food_servings")
data class FoodServingEntity(
    @PrimaryKey val id: String,
    val foodProductId: String,
    val label: String,
    val grams: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val loggedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "food_entries")
data class FoodEntryEntity(
    @PrimaryKey val id: String,
    val mealId: String?,
    val foodProductId: String?,
    val name: String,
    val brand: String?,
    val mealType: String,
    val grams: Double,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydratesGrams: Double,
    val sugarGrams: Double,
    val fatGrams: Double,
    val saturatedFatGrams: Double,
    val fiberGrams: Double,
    val saltGrams: Double,
    val sodiumMilligrams: Double?,
    val micronutrientsJson: String? = null,
    val consumedAt: Long,
    val notes: String,
    val dataQuality: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "nutrients")
data class NutrientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val nutrientType: String
)

@Entity(tableName = "food_nutrients")
data class FoodNutrientEntity(
    @PrimaryKey val id: String,
    val foodProductId: String,
    val nutrientId: String,
    val amountPer100g: Double?,
    val dataQuality: String,
    val source: String?
)

@Entity(tableName = "daily_nutrition_summaries")
data class DailyNutritionSummaryEntity(
    @PrimaryKey val id: String,
    val dayStartMillis: Long,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydratesGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double,
    val waterMilliliters: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val machineType: String,
    val gym: String?,
    val machineCode: String?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val gym: String?,
    val machineCode: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "machine_settings")
data class MachineSettingEntity(
    @PrimaryKey val id: String,
    val machineId: String,
    val label: String,
    val value: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val durationMinutes: Int?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val focus: String,
    val estimatedMinutes: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "workout_plan_exercises")
data class WorkoutPlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetRepMin: Int,
    val targetRepMax: Int,
    val restSeconds: Int,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val machineId: String?,
    val sortOrder: Int = 0,
    val notes: String,
    val painOrDiscomfort: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val weightKg: Double,
    val repetitions: Int,
    val setCount: Int,
    val isWarmup: Boolean,
    val isPerSide: Boolean,
    val setType: String = "Normal",
    val completedAt: Long? = null,
    val restSeconds: Int = 90,
    val rpe: Int?,
    val rir: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "body_weight_entries")
data class BodyWeightEntryEntity(
    @PrimaryKey val id: String,
    val measuredAt: Long,
    val weightKg: Double,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "recommendations")
data class RecommendationEntity(
    @PrimaryKey val id: String,
    val category: String,
    val message: String,
    val rationale: String,
    val confidence: Double,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "sync_queue_items")
data class SyncQueueItemEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val idempotencyKey: String,
    val retryCount: Int,
    val nextAttemptAt: Long?,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "image_analysis_results")
data class ImageAnalysisResultEntity(
    @PrimaryKey val id: String,
    val localImageUri: String?,
    val remoteReference: String?,
    val resultJson: String,
    val userConfirmed: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)

@Entity(tableName = "barcode_scan_results")
data class BarcodeScanResultEntity(
    @PrimaryKey val id: String,
    val barcode: String,
    val found: Boolean,
    val productId: String?,
    val rawResponseJson: String?,
    val scannedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val syncStatus: SyncStatus,
    val serverVersion: Long?
)
