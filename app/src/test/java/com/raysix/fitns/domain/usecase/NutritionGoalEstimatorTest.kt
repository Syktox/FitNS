package com.raysix.fitns.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionGoalEstimatorTest {

    @Test
    fun `estimates maintain for moderate activity`() {
        val goal = NutritionGoalEstimator.estimate(weightKg = 80.0, goal = "Maintain", activityLevel = "Moderate")

        assertEquals(2550.0, goal.caloriesKcal, 0.001)
        assertEquals(145.0, goal.proteinGrams, 0.001)
        assertEquals(65.0, goal.fatGrams, 0.001)
        assertEquals(30.0, goal.fiberGrams, 0.001)
        assertEquals(2750.0, goal.waterMilliliters, 0.001)
    }

    @Test
    fun `estimates lower calories for lose fat goal`() {
        val maintain = NutritionGoalEstimator.estimate(weightKg = 80.0, goal = "Maintain", activityLevel = "Moderate")
        val goal = NutritionGoalEstimator.estimate(weightKg = 80.0, goal = "Lose Fat", activityLevel = "Moderate")

        assertEquals(2200.0, goal.caloriesKcal, 0.001)
        assertTrue(goal.caloriesKcal < maintain.caloriesKcal)
    }

    @Test
    fun `estimates higher protein for build muscle goal`() {
        val goal = NutritionGoalEstimator.estimate(weightKg = 80.0, goal = "Build Muscle", activityLevel = "Moderate")

        assertEquals(2800.0, goal.caloriesKcal, 0.001)
        assertEquals(160.0, goal.proteinGrams, 0.001)
    }

    @Test
    fun `uses defaults when weight is missing`() {
        val goal = NutritionGoalEstimator.estimate(weightKg = null, goal = "Maintain", activityLevel = "Moderate")

        assertEquals(2400.0, goal.caloriesKcal, 0.001)
        assertTrue(goal.proteinGrams > 0.0)
    }

    @Test
    fun `clamps calories to a sensible minimum`() {
        val goal = NutritionGoalEstimator.estimate(weightKg = 20.0, goal = "Lose Fat", activityLevel = "Low")

        assertTrue(goal.caloriesKcal >= 1400.0)
    }
}
