package com.raysix.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.raysix.fitns.data.local.entity.ExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutEntity
import com.raysix.fitns.data.local.entity.WorkoutExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM exercises WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM workout_sets WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeWorkoutSets(): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_exercises WHERE deletedAt IS NULL ORDER BY createdAt DESC, sortOrder ASC")
    fun observeWorkoutExercises(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_plans WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeWorkoutPlans(): Flow<List<WorkoutPlanEntity>>

    @Query("SELECT * FROM workout_plan_exercises WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeWorkoutPlanExercises(): Flow<List<WorkoutPlanExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId AND deletedAt IS NULL")
    suspend fun workoutExercisesForWorkout(workoutId: String): List<WorkoutExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutExercises(workoutExercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSet(set: WorkoutSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutSets(sets: List<WorkoutSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutPlan(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutPlanExercises(exercises: List<WorkoutPlanExerciseEntity>)

    @Query("UPDATE workout_plan_exercises SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE planId = :planId")
    suspend fun softDeleteWorkoutPlanExercises(planId: String, deletedAt: Long)

    @Query("UPDATE workout_plans SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :planId")
    suspend fun softDeleteWorkoutPlan(planId: String, deletedAt: Long)

    @Transaction
    suspend fun upsertWorkoutPlanWithExercises(
        plan: WorkoutPlanEntity,
        exercises: List<WorkoutPlanExerciseEntity>
    ) {
        val now = System.currentTimeMillis()
        softDeleteWorkoutPlanExercises(plan.id, now)
        upsertWorkoutPlan(plan)
        upsertWorkoutPlanExercises(exercises)
    }

    @Transaction
    suspend fun softDeleteWorkoutPlanCascade(planId: String, deletedAt: Long) {
        softDeleteWorkoutPlan(planId, deletedAt)
        softDeleteWorkoutPlanExercises(planId, deletedAt)
    }

    @Transaction
    suspend fun addWorkoutSet(
        exercise: ExerciseEntity,
        workout: WorkoutEntity,
        workoutExercise: WorkoutExerciseEntity,
        set: WorkoutSetEntity
    ) {
        upsertExercise(exercise)
        upsertWorkout(workout)
        upsertWorkoutExercise(workoutExercise)
        upsertWorkoutSet(set)
    }

    @Transaction
    suspend fun addWorkoutSession(
        exercises: List<ExerciseEntity>,
        workout: WorkoutEntity,
        workoutExercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>
    ) {
        exercises.forEach { upsertExercise(it) }
        upsertWorkout(workout)
        upsertWorkoutExercises(workoutExercises)
        upsertWorkoutSets(sets)
    }

    @Query("UPDATE workouts SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :workoutId")
    suspend fun softDeleteWorkout(workoutId: String, deletedAt: Long)

    @Query("UPDATE workout_exercises SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE workoutId = :workoutId")
    suspend fun softDeleteWorkoutExercises(workoutId: String, deletedAt: Long)

    @Query("UPDATE workout_sets SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE workoutExerciseId IN (:workoutExerciseIds)")
    suspend fun softDeleteWorkoutSets(workoutExerciseIds: List<String>, deletedAt: Long)

    @Transaction
    suspend fun softDeleteWorkoutCascade(workoutId: String, workoutExerciseIds: List<String>, deletedAt: Long) {
        softDeleteWorkout(workoutId, deletedAt)
        softDeleteWorkoutExercises(workoutId, deletedAt)
        if (workoutExerciseIds.isNotEmpty()) {
            softDeleteWorkoutSets(workoutExerciseIds, deletedAt)
        }
    }
}
