package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.PersonalRecordEvent
import com.raysix.fitns.domain.model.PersonalRecordType
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutSetType
import kotlin.math.roundToInt
import javax.inject.Inject

class PersonalRecordDetector @Inject constructor(
    private val estimatedOneRepMaxCalculator: EstimatedOneRepMaxCalculator
) {
    fun detect(session: ActiveWorkoutSession, history: List<WorkoutLogEntry>): List<PersonalRecordEvent> {
        val existingByExercise = history.groupBy { it.exercise.id }
        return session.exercises.flatMap { activeExercise ->
            val currentSets = activeExercise.sets.filter {
                it.completedAt != null && it.setType != WorkoutSetType.WarmUp
            }
            val historicalSets = existingByExercise[activeExercise.exercise.id]
                .orEmpty()
                .flatMap { it.sets }
                .filter { it.setType != WorkoutSetType.WarmUp }
            val events = mutableListOf<PersonalRecordEvent>()

            val currentMaxWeight = currentSets.maxOfOrNull { it.weightKg }
            val historicalMaxWeight = historicalSets.maxOfOrNull { it.weightKg } ?: 0.0
            if (currentMaxWeight != null && currentMaxWeight > historicalMaxWeight) {
                events += PersonalRecordEvent(
                    exerciseName = activeExercise.exercise.name,
                    type = PersonalRecordType.HighestWeight,
                    value = currentMaxWeight,
                    unit = "kg"
                )
            }

            val bestRepsAtWeight = currentSets.maxByOrNull { it.weightKg * 10_000 + it.repetitions }
            if (bestRepsAtWeight != null) {
                val previousBestRepsAtWeight = historicalSets
                    .filter { it.weightKg == bestRepsAtWeight.weightKg }
                    .maxOfOrNull { it.repetitions } ?: 0
                if (bestRepsAtWeight.repetitions > previousBestRepsAtWeight) {
                    events += PersonalRecordEvent(
                        exerciseName = activeExercise.exercise.name,
                        type = PersonalRecordType.HighestRepsAtWeight,
                        value = bestRepsAtWeight.repetitions.toDouble(),
                        unit = "reps"
                    )
                }
            }

            val currentBestOneRepMax = currentSets.maxOfOrNull {
                estimatedOneRepMaxCalculator.calculate(it.weightKg, it.repetitions)
            }
            val historicalBestOneRepMax = historicalSets.maxOfOrNull {
                estimatedOneRepMaxCalculator.calculate(it.weightKg, it.repetitions)
            } ?: 0.0
            if (currentBestOneRepMax != null && currentBestOneRepMax > historicalBestOneRepMax) {
                events += PersonalRecordEvent(
                    exerciseName = activeExercise.exercise.name,
                    type = PersonalRecordType.HighestEstimatedOneRepMax,
                    value = currentBestOneRepMax.roundToInt().toDouble(),
                    unit = "kg"
                )
            }

            events
        } + detectSessionVolume(session, history)
    }

    private fun detectSessionVolume(session: ActiveWorkoutSession, history: List<WorkoutLogEntry>): List<PersonalRecordEvent> {
        val currentVolume = session.exercises.sumOf { exercise ->
            exercise.sets
                .filter { it.completedAt != null && it.setType != WorkoutSetType.WarmUp }
                .sumOf { it.weightKg * it.repetitions }
        }
        val historicalBest = history
            .groupBy { it.id }
            .values
            .maxOfOrNull { entries ->
                entries.sumOf { entry ->
                    entry.sets
                        .filter { it.setType != WorkoutSetType.WarmUp }
                        .sumOf { set -> set.weightKg * set.repetitions * set.sets }
                }
            } ?: 0.0

        if (currentVolume <= historicalBest || currentVolume <= 0.0) return emptyList()
        return listOf(
            PersonalRecordEvent(
                exerciseName = session.name,
                type = PersonalRecordType.HighestSessionVolume,
                value = currentVolume.roundToInt().toDouble(),
                unit = "kg"
            )
        )
    }
}
