package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ProgressionAction
import com.raysix.fitns.domain.model.ProgressionRecommendation
import com.raysix.fitns.domain.model.WorkoutSetInput

class WorkoutProgressionCalculator {
    fun recommend(
        completedSets: List<WorkoutSetInput>,
        targetRepMin: Int,
        targetRepMax: Int
    ): ProgressionRecommendation {
        if (completedSets.isEmpty()) {
            return ProgressionRecommendation(
                ProgressionAction.InsufficientData,
                "No working sets have been logged yet."
            )
        }

        val allAtTop = completedSets.all { it.repetitions >= targetRepMax }
        val anyBelowMin = completedSets.any { it.repetitions < targetRepMin }
        val highEffort = completedSets.any { (it.rpe ?: 0) >= 9 }

        return when {
            allAtTop && !highEffort -> ProgressionRecommendation(
                ProgressionAction.IncreaseWeight,
                "All sets reached the top of the rep target with controlled effort."
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
                ProgressionAction.ReduceWeight,
                "The lower rep target was missed."
            )
            else -> ProgressionRecommendation(
                ProgressionAction.IncreaseRepetitions,
                "Keep the weight and build reps gradually."
            )
        }
    }
}
