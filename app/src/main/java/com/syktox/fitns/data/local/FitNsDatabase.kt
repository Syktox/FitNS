package com.syktox.fitns.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.syktox.fitns.core.serialization.MicronutrientsCodec
import com.syktox.fitns.data.local.dao.BodyWeightDao
import com.syktox.fitns.data.local.dao.FoodDao
import com.syktox.fitns.data.local.dao.ProfileDao
import com.syktox.fitns.data.local.dao.SyncQueueDao
import com.syktox.fitns.data.local.dao.WorkoutDao
import com.syktox.fitns.data.local.entity.BarcodeScanResultEntity
import com.syktox.fitns.data.local.entity.BodyWeightEntryEntity
import com.syktox.fitns.data.local.entity.DailyNutritionSummaryEntity
import com.syktox.fitns.data.local.entity.ExerciseEntity
import com.syktox.fitns.data.local.entity.FoodEntryEntity
import com.syktox.fitns.data.local.entity.FoodNutrientEntity
import com.syktox.fitns.data.local.entity.FoodProductEntity
import com.syktox.fitns.data.local.entity.FoodServingEntity
import com.syktox.fitns.data.local.entity.ImageAnalysisResultEntity
import com.syktox.fitns.data.local.entity.MachineEntity
import com.syktox.fitns.data.local.entity.MachineSettingEntity
import com.syktox.fitns.data.local.entity.MealEntity
import com.syktox.fitns.data.local.entity.NutrientEntity
import com.syktox.fitns.data.local.entity.NutrientTargetEntity
import com.syktox.fitns.data.local.entity.NutritionGoalEntity
import com.syktox.fitns.data.local.entity.RecommendationEntity
import com.syktox.fitns.data.local.entity.SyncQueueItemEntity
import com.syktox.fitns.data.local.entity.SyncStatus
import com.syktox.fitns.data.local.entity.UserProfileEntity
import com.syktox.fitns.data.local.entity.WorkoutEntity
import com.syktox.fitns.data.local.entity.WorkoutExerciseEntity
import com.syktox.fitns.data.local.entity.WorkoutPlanEntity
import com.syktox.fitns.data.local.entity.WorkoutPlanExerciseEntity
import com.syktox.fitns.data.local.entity.WorkoutSetEntity
import com.syktox.fitns.domain.model.Micronutrients

@Database(
    entities = [
        UserProfileEntity::class,
        NutritionGoalEntity::class,
        FoodProductEntity::class,
        FoodServingEntity::class,
        FoodEntryEntity::class,
        MealEntity::class,
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
    version = 3,
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

