package com.raysix.fitns.data.repository

import com.raysix.fitns.data.local.entity.ExerciseEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.data.local.entity.WorkoutEntity
import com.raysix.fitns.data.local.entity.WorkoutExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanEntity
import com.raysix.fitns.data.local.entity.WorkoutPlanExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutSetEntity
import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import com.raysix.fitns.domain.model.WorkoutSetType
import java.util.UUID

fun ExerciseEntity.toDomain(
    lastWeightKg: Double? = null,
    lastRepetitions: Int? = null,
    lastSets: Int? = null,
    personalBestKg: Double? = lastWeightKg
): Exercise {
    return Exercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        machineType = machineType,
        gym = gym.orEmpty(),
        lastWeightKg = lastWeightKg,
        lastRepetitions = lastRepetitions,
        lastSets = lastSets,
        personalBestKg = personalBestKg
    )
}

fun Exercise.toEntity(now: Long = System.currentTimeMillis()): ExerciseEntity {
    return ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        machineType = machineType,
        gym = gym.ifBlank { null },
        machineCode = null,
        notes = "",
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun WorkoutSetInput.toEntity(workoutExerciseId: String, now: Long = System.currentTimeMillis()): WorkoutSetEntity {
    return WorkoutSetEntity(
        id = UUID.randomUUID().toString(),
        workoutExerciseId = workoutExerciseId,
        weightKg = weightKg,
        repetitions = repetitions,
        setCount = sets,
        isWarmup = setType == WorkoutSetType.WarmUp,
        isPerSide = isPerSide,
        setType = setType.name,
        completedAt = completedAt,
        restSeconds = restSeconds,
        rpe = rpe,
        rir = rir,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun WorkoutPlan.toEntity(now: Long = System.currentTimeMillis()): WorkoutPlanEntity {
    return WorkoutPlanEntity(
        id = id,
        name = name,
        focus = focus,
        estimatedMinutes = estimatedMinutes,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun WorkoutPlanExercise.toEntity(planId: String, sortOrder: Int, now: Long = System.currentTimeMillis()): WorkoutPlanExerciseEntity {
    return WorkoutPlanExerciseEntity(
        id = "$planId-${exercise.id}-$sortOrder",
        planId = planId,
        exerciseId = exercise.id,
        sortOrder = sortOrder,
        targetSets = targetSets,
        targetRepMin = targetRepMin,
        targetRepMax = targetRepMax,
        restSeconds = restSeconds,
        notes = notes,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun ActiveWorkoutSet.toEntity(workoutExerciseId: String, now: Long = System.currentTimeMillis()): WorkoutSetEntity {
    return WorkoutSetEntity(
        id = id,
        workoutExerciseId = workoutExerciseId,
        weightKg = weightKg,
        repetitions = repetitions,
        setCount = setNumber,
        isWarmup = setType == WorkoutSetType.WarmUp,
        isPerSide = false,
        setType = setType.name,
        completedAt = completedAt,
        restSeconds = restSeconds,
        rpe = rpe,
        rir = rir,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun newWorkoutEntity(id: String, now: Long, endedAt: Long? = now): WorkoutEntity {
    return WorkoutEntity(
        id = id,
        startedAt = now,
        endedAt = endedAt,
        durationMinutes = null,
        notes = "",
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun newWorkoutExerciseEntity(
    id: String,
    workoutId: String,
    exerciseId: String,
    notes: String,
    now: Long,
    sortOrder: Int = 0
): WorkoutExerciseEntity {
    return WorkoutExerciseEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        machineId = null,
        sortOrder = sortOrder,
        notes = notes,
        painOrDiscomfort = null,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun ActiveWorkoutExercise.toWorkoutExerciseEntity(workoutId: String, now: Long = System.currentTimeMillis()): WorkoutExerciseEntity {
    return newWorkoutExerciseEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = exercise.id,
        notes = "",
        now = now,
        sortOrder = sortOrder
    )
}
