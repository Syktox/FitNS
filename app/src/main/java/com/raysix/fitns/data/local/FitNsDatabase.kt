package com.raysix.fitns.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.raysix.fitns.core.serialization.MicronutrientsCodec
import com.raysix.fitns.data.local.dao.BodyWeightDao
import com.raysix.fitns.data.local.dao.FoodDao
import com.raysix.fitns.data.local.dao.ProfileDao
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.data.local.dao.WorkoutDao
import com.raysix.fitns.data.local.entity.BarcodeScanResultEntity
import com.raysix.fitns.data.local.entity.BodyWeightEntryEntity
import com.raysix.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.raysix.fitns.data.local.entity.ExerciseEntity
import com.raysix.fitns.data.local.entity.FoodEntryEntity
import com.raysix.fitns.data.local.entity.FoodNutrientEntity
import com.raysix.fitns.data.local.entity.FoodProductEntity
import com.raysix.fitns.data.local.entity.FoodServingEntity
import com.raysix.fitns.data.local.entity.ImageAnalysisResultEntity
import com.raysix.fitns.data.local.entity.MachineEntity
import com.raysix.fitns.data.local.entity.MachineSettingEntity
import com.raysix.fitns.data.local.entity.MealEntity
import com.raysix.fitns.data.local.entity.NutrientEntity
import com.raysix.fitns.data.local.entity.NutrientTargetEntity
import com.raysix.fitns.data.local.entity.NutritionGoalEntity
import com.raysix.fitns.data.local.entity.RecommendationEntity
import com.raysix.fitns.data.local.entity.SavedMealEntity
import com.raysix.fitns.data.local.entity.SavedMealItemEntity
import com.raysix.fitns.data.local.entity.SyncQueueItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.data.local.entity.UserProfileEntity
import com.raysix.fitns.data.local.entity.WorkoutEntity
import com.raysix.fitns.data.local.entity.WorkoutExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutSetEntity
import com.raysix.fitns.domain.model.Micronutrients

@Database(
    entities = [
        UserProfileEntity::class,
        NutritionGoalEntity::class,
        FoodProductEntity::class,
        FoodServingEntity::class,
        FoodEntryEntity::class,
        MealEntity::class,
        SavedMealEntity::class,
        SavedMealItemEntity::class,
        NutrientEntity::class,
        FoodNutrientEntity::class,
        NutrientTargetEntity::class,
        DailyNutritionSummaryEntity::class,
        ExerciseEntity::class,
        MachineEntity::class,
        MachineSettingEntity::class,
        WorkoutEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutExerciseEntity::class,
        WorkoutSetEntity::class,
        BodyWeightEntryEntity::class,
        RecommendationEntity::class,
        SyncQueueItemEntity::class,
        ImageAnalysisResultEntity::class,
        BarcodeScanResultEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(FitNsTypeConverters::class)
abstract class FitNsDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun profileDao(): ProfileDao
}

class FitNsTypeConverters {
    @TypeConverter
    fun syncStatusToString(value: SyncStatus): String = value.name

    @TypeConverter
    fun stringToSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun micronutrientsToString(value: Micronutrients): String? = MicronutrientsCodec.encode(value)

    @TypeConverter
    fun stringToMicronutrients(value: String?): Micronutrients = MicronutrientsCodec.decode(value)
}
