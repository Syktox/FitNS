package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ProgressionAction
import com.raysix.fitns.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutProgressionCalculatorTest {
    private val calculator = WorkoutProgressionCalculator()

    @Test
    fun recommend_increaseWeightWhenAllSetsAtTopAndEffortControlled() {
        val result = calculator.recommend(
            completedSets = listOf(
                WorkoutSetInput(weightKg = 50.0, repetitions = 12, rpe = 8),
                WorkoutSetInput(weightKg = 50.0, repetitions = 12, rpe = 8),
                WorkoutSetInput(weightKg = 50.0, repetitions = 12, rpe = 8)
            ),
            targetRepMin = 8,
            targetRepMax = 12
        )

        assertEquals(ProgressionAction.IncreaseWeight, result.action)
        assertEquals(52.5, result.suggestedWeightKg ?: 0.0, 0.001)
    }

    @Test
    fun recommend_considerRecoveryWhenBelowMinAndHighEffort() {
        val result = calculator.recommend(
            completedSets = listOf(
                WorkoutSetInput(weightKg = 50.0, repetitions = 6, rpe = 10)
            ),
            targetRepMin = 8,
            targetRepMax = 12
        )

        assertEquals(ProgressionAction.ConsiderRecovery, result.action)
    }

    @Test
    fun recommend_keepWeightWhenTopRepsButRirShowsHighEffort() {
        val result = calculator.recommend(
            completedSets = listOf(
                WorkoutSetInput(weightKg = 100.0, repetitions = 12, rpe = null, rir = 0),
                WorkoutSetInput(weightKg = 100.0, repetitions = 12, rpe = null, rir = 1),
                WorkoutSetInput(weightKg = 100.0, repetitions = 12, rpe = null, rir = 1)
            ),
            targetRepMin = 8,
            targetRepMax = 12
        )

        assertEquals(ProgressionAction.KeepWeight, result.action)
    }
}
