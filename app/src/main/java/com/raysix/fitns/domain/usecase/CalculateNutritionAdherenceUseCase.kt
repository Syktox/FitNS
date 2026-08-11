package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.CoachMetric
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.NutritionAdherenceResult
import javax.inject.Inject

class CalculateNutritionAdherenceUseCase @Inject constructor() {
    operator fun invoke(dashboard: DailyNutritionDashboard, workoutSetCount: Int): NutritionAdherenceResult {
        val calorieRatio = dashboard.total.caloriesKcal.ratioTo(dashboard.goal.caloriesKcal)
        val proteinRatio = dashboard.total.proteinGrams.ratioTo(dashboard.goal.proteinGrams)
        val waterRatio = dashboard.waterMilliliters.ratioTo(dashboard.goal.waterMilliliters)
        val nutritionScore = listOf(
            calorieRatio.scoreForRange(0.75, 1.08),
            proteinRatio.scoreForMinimum(0.75),
            waterRatio.scoreForMinimum(0.7)
        ).average()
        val focus = when {
            dashboard.entries.isEmpty() -> "Log your first meal so the targets become actionable."
            proteinRatio < 0.65 -> "Prioritize protein at the next meal."
            waterRatio < 0.6 -> "Add water now and check hydration again later."
            calorieRatio > 1.1 -> "Keep the rest of the day lighter and protein-forward."
            workoutSetCount == 0 -> "Add a short strength session or log today's completed sets."
            else -> "Stay consistent and finish the day close to target."
        }
        return NutritionAdherenceResult(
            score = nutritionScore,
            focus = focus,
            metrics = listOf(
                CoachMetric("Calories", dashboard.total.caloriesKcal, dashboard.goal.caloriesKcal, "kcal", calorieRatio.statusForCalories()),
                CoachMetric("Protein", dashboard.total.proteinGrams, dashboard.goal.proteinGrams, "g", proteinRatio.statusForMinimum()),
                CoachMetric("Water", dashboard.waterMilliliters, dashboard.goal.waterMilliliters, "ml", waterRatio.statusForMinimum()),
                CoachMetric("Workout", workoutSetCount.toDouble(), 1.0, "sets", if (workoutSetCount > 0) "Done" else "Open")
            )
        )
    }

    private fun Double.ratioTo(target: Double): Double = if (target <= 0.0) 0.0 else this / target

    private fun Double.scoreForMinimum(minimum: Double): Double = (this / minimum).coerceIn(0.0, 1.0)

    private fun Double.scoreForRange(minimum: Double, maximum: Double): Double {
        return when {
            this < minimum -> (this / minimum).coerceIn(0.0, 1.0)
            this <= maximum -> 1.0
            else -> (1.0 - ((this - maximum) / 0.4)).coerceIn(0.0, 1.0)
        }
    }

    private fun Double.statusForMinimum(): String {
        return when {
            this >= 1.0 -> "Hit"
            this >= 0.75 -> "Close"
            else -> "Open"
        }
    }

    private fun Double.statusForCalories(): String {
        return when {
            this in 0.85..1.05 -> "On target"
            this < 0.85 -> "Room left"
            else -> "Over"
        }
    }
}
