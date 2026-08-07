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

data class WorkoutSetInput(
    val weightKg: Double,
    val repetitions: Int,
    val rpe: Int?,
    val sets: Int = 1,
    val isPerSide: Boolean = false
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
    val reason: String
)
