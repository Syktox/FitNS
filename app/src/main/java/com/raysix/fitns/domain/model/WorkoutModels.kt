package com.raysix.fitns.domain.model

import java.util.UUID

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val machineType: String,
    val gym: String = "",
    val lastWeightKg: Double? = null,
    val lastRepetitions: Int? = null,
    val lastSets: Int? = null,
    val personalBestKg: Double? = null
)

data class WorkoutTemplate(
    val id: String,
    val name: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exercises: List<Exercise>
)

data class WorkoutPlanExercise(
    val exercise: Exercise,
    val targetSets: Int,
    val targetRepMin: Int,
    val targetRepMax: Int,
    val restSeconds: Int,
    val notes: String = ""
)

data class WorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exercises: List<WorkoutPlanExercise>
)

enum class WorkoutSetType(val label: String) {
    WarmUp("Warm-up"),
    Normal("Normal"),
    DropSet("Drop Set"),
    Failure("Failure"),
    Amrap("AMRAP")
}

data class WorkoutSetInput(
    val weightKg: Double,
    val repetitions: Int,
    val rpe: Int?,
    val rir: Int? = null,
    val sets: Int = 1,
    val isPerSide: Boolean = false,
    val setType: WorkoutSetType = WorkoutSetType.Normal,
    val completedAt: Long? = null,
    val restSeconds: Int = 90
)

data class WorkoutLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sets: List<WorkoutSetInput>,
    val notes: String = "",
    val loggedAt: Long = System.currentTimeMillis()
) {
    val volumeKg: Double
        get() = sets.sumOf { set ->
            val multiplier = if (set.isPerSide) 2 else 1
            set.weightKg * set.repetitions * set.sets * multiplier
        }
}

enum class ProgressionAction {
    IncreaseWeight,
    KeepWeight,
    ReduceWeight,
    IncreaseRepetitions,
    ConsiderRecovery,
    InsufficientData
}

data class ProgressionRecommendation(
    val action: ProgressionAction,
    val reason: String,
    val suggestedWeightKg: Double? = null
)

data class PreviousPerformance(
    val exerciseId: String,
    val weightKg: Double,
    val repetitions: Int,
    val loggedAt: Long
)

data class ActiveWorkoutSet(
    val id: String = UUID.randomUUID().toString(),
    val setNumber: Int,
    val weightKg: Double = 0.0,
    val repetitions: Int = 0,
    val rpe: Int? = null,
    val rir: Int? = null,
    val setType: WorkoutSetType = WorkoutSetType.Normal,
    val completedAt: Long? = null,
    val previousPerformance: PreviousPerformance? = null,
    val restSeconds: Int = 90
)

data class ActiveWorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val exercise: Exercise,
    val sortOrder: Int,
    val targetRepMin: Int,
    val targetRepMax: Int,
    val restSeconds: Int,
    val sets: List<ActiveWorkoutSet>
)

data class ActiveWorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val sourcePlanId: String?,
    val name: String,
    val startedAt: Long = System.currentTimeMillis(),
    val exercises: List<ActiveWorkoutExercise>
) {
    val completedSetCount: Int
        get() = exercises.sumOf { exercise -> exercise.sets.count { it.completedAt != null } }

    val totalSetCount: Int
        get() = exercises.sumOf { it.sets.size }
}

enum class PersonalRecordType(val label: String) {
    HighestWeight("Highest Weight"),
    HighestRepsAtWeight("Highest Reps at Weight"),
    HighestEstimatedOneRepMax("Highest Estimated 1RM"),
    HighestSessionVolume("Highest Session Volume")
}

data class PersonalRecordEvent(
    val exerciseName: String,
    val type: PersonalRecordType,
    val value: Double,
    val unit: String
)
