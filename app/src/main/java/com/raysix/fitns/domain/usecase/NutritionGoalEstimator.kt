package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.NutritionGoal

object NutritionGoalEstimator {

    fun estimate(weightKg: Double?, goal: String, activityLevel: String): NutritionGoal {
        val bodyWeight = weightKg?.takeIf { it > 0.0 } ?: 75.0
        val activityMultiplier = when (activityLevel.lowercase()) {
            "low" -> 28.0
            "high" -> 36.0
            else -> 32.0
        }
        val goalAdjustment = when (goal.lowercase()) {
            "lose fat" -> -350.0
            "build muscle" -> 250.0
            else -> 0.0
        }
        val calories = (bodyWeight * activityMultiplier + goalAdjustment).coerceAtLeast(1400.0)
        val protein = bodyWeight * if (goal.equals("Build Muscle", ignoreCase = true)) 2.0 else 1.8
        val fat = bodyWeight * 0.8
        val carbs = ((calories - protein * 4.0 - fat * 9.0) / 4.0).coerceAtLeast(80.0)

        return NutritionGoal(
            caloriesKcal = calories.roundToNearest(25.0),
            proteinGrams = protein.roundToNearest(5.0),
            carbohydrateGrams = carbs.roundToNearest(5.0),
            fatGrams = fat.roundToNearest(5.0),
            fiberGrams = 30.0,
            waterMilliliters = (bodyWeight * 35.0).roundToNearest(250.0)
        )
    }

    private fun Double.roundToNearest(step: Double): Double {
        return kotlin.math.round(this / step) * step
    }
}
