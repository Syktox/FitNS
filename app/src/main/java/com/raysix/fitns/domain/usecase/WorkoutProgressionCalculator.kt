package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ProgressionAction
import com.raysix.fitns.domain.model.ProgressionRecommendation
import com.raysix.fitns.domain.model.WorkoutSetInput

class WorkoutProgressionCalculator {
    fun recommend(
        completedSets: List<WorkoutSetInput>,
        targetRepMin: Int,
        targetRepMax: Int,
        weightIncrementKg: Double = 2.5
    ): ProgressionRecommendation {
        if (completedSets.isEmpty()) {
            return ProgressionRecommendation(
                ProgressionAction.InsufficientData,
                "No working sets have been logged yet."
            )
        }

        val workingSets = completedSets.filter { it.setType != com.raysix.fitns.domain.model.WorkoutSetType.WarmUp }
        if (workingSets.isEmpty()) {
            return ProgressionRecommendation(
                ProgressionAction.InsufficientData,
                "Only warm-up sets have been logged so far."
            )
        }

        val allAtTop = workingSets.all { it.repetitions >= targetRepMax }
        val anyBelowMin = workingSets.any { it.repetitions < targetRepMin }
        val highEffort = workingSets.any { (it.rpe ?: 0) >= 9 || (it.rir ?: Int.MAX_VALUE) <= 1 }
        val currentWeight = workingSets.maxOfOrNull { it.weightKg }

        return when {
            allAtTop && !highEffort -> ProgressionRecommendation(
                ProgressionAction.IncreaseWeight,
                currentWeight?.let { "All sets reached the top of the rep target. Increase weight to ${it + weightIncrementKg} kg." }
                    ?: "All sets reached the top of the rep target with controlled effort.",
                suggestedWeightKg = currentWeight?.plus(weightIncrementKg)
            )
            allAtTop -> ProgressionRecommendation(
                ProgressionAction.KeepWeight,
                "The reps are there, but effort is high. Stabilize before increasing weight."
            )
            anyBelowMin && highEffort -> ProgressionRecommendation(
                ProgressionAction.ConsiderRecovery,
                "Several signals point to high fatigue. Check recovery before pushing harder."
            )
            anyBelowMin -> ProgressionRecommendation(
                ProgressionAction.KeepWeight,
                "The lower rep target was missed. Keep the current load and rebuild reps."
            )
            else -> ProgressionRecommendation(
                ProgressionAction.IncreaseRepetitions,
                "Keep the weight and build reps gradually."
            )
        }
    }
}
