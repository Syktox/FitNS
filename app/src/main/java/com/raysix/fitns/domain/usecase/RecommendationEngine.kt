package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.RecommendationItem
import com.raysix.fitns.domain.model.RecommendationSeverity
import com.raysix.fitns.domain.model.WorkoutLogEntry
import kotlin.math.abs

class RecommendationEngine(
    private val bodyWeightTrendCalculator: BodyWeightTrendCalculator = BodyWeightTrendCalculator()
) {
    fun generate(
        dashboard: DailyNutritionDashboard,
        bodyWeights: List<BodyWeightLogEntry>,
        workouts: List<WorkoutLogEntry>
    ): List<RecommendationItem> {
        val recommendations = mutableListOf<RecommendationItem>()
        recommendations += nutritionRecommendations(dashboard)
        recommendations += workoutRecommendations(workouts)
        recommendations += bodyWeightRecommendations(bodyWeights)
        if (recommendations.isEmpty()) {
            recommendations += RecommendationItem(
                category = "Data Quality",
                message = "No specific recommendation yet.",
                rationale = "Log several complete days so trends become more reliable.",
                severity = RecommendationSeverity.Info,
                confidence = 0.3
            )
        }
        return recommendations
    }

    private fun nutritionRecommendations(dashboard: DailyNutritionDashboard): List<RecommendationItem> {
        val result = mutableListOf<RecommendationItem>()
        val proteinRemaining = dashboard.goal.proteinGrams - dashboard.total.proteinGrams
        val fiberRemaining = dashboard.goal.fiberGrams - dashboard.total.fiberGrams
        val calorieRatio = if (dashboard.goal.caloriesKcal <= 0.0) 0.0 else dashboard.total.caloriesKcal / dashboard.goal.caloriesKcal

        if (proteinRemaining > 20.0) {
            result += RecommendationItem(
                category = "Protein",
                message = "You are about ${proteinRemaining.toInt()} g short of your protein goal today.",
                rationale = "Logged so far: ${dashboard.total.proteinGrams.toInt()} g of ${dashboard.goal.proteinGrams.toInt()} g.",
                severity = RecommendationSeverity.Attention,
                confidence = 0.75
            )
        }

        if (fiberRemaining > 8.0) {
            result += RecommendationItem(
                category = "Fiber",
                message = "Your fiber intake is still below target today.",
                rationale = "About ${fiberRemaining.toInt()} g remain. Product data may be incomplete.",
                severity = RecommendationSeverity.Info,
                confidence = 0.65
            )
        }

        if (calorieRatio > 1.1) {
            result += RecommendationItem(
                category = "Calories",
                message = "You are above your calorie target today.",
                rationale = "One day is not a trend. Review this across multiple days.",
                severity = RecommendationSeverity.Info,
                confidence = 0.55
            )
        } else if (calorieRatio in 0.85..1.05 && dashboard.entries.isNotEmpty()) {
            result += RecommendationItem(
                category = "Calories",
                message = "Today's calorie intake is close to your target.",
                rationale = "Logged calories and your target are currently in a close range.",
                severity = RecommendationSeverity.Positive,
                confidence = 0.55
            )
        }

        if (dashboard.entries.isEmpty()) {
            result += RecommendationItem(
                category = "Data Quality",
                message = "No foods have been logged today.",
                rationale = "Calories, macros, and micronutrients cannot be evaluated without entries.",
                severity = RecommendationSeverity.Info,
                confidence = 0.9
            )
        }

        return result
    }

    private fun workoutRecommendations(workouts: List<WorkoutLogEntry>): List<RecommendationItem> {
        if (workouts.isEmpty()) {
            return listOf(
                RecommendationItem(
                    category = "Workout",
                    message = "No workouts have been logged yet.",
                    rationale = "Progression and workout frequency require multiple entries.",
                    severity = RecommendationSeverity.Info,
                    confidence = 0.85
                )
            )
        }

        val latest = workouts.first()
        val latestSet = latest.sets.firstOrNull()
        return if (latestSet != null && latestSet.repetitions >= 12 && (latestSet.rpe ?: 0) <= 8) {
            listOf(
                RecommendationItem(
                    category = "Progression",
                    message = "A small increase may make sense for ${latest.exercise.name}.",
                    rationale = "The latest set reached at least 12 reps at moderate effort.",
                    severity = RecommendationSeverity.Positive,
                    confidence = 0.6
                )
            )
        } else {
            listOf(
                RecommendationItem(
                    category = "Workout",
                    message = "Keep building your workout history.",
                    rationale = "Several sessions per exercise make progression recommendations more reliable.",
                    severity = RecommendationSeverity.Info,
                    confidence = 0.45
                )
            )
        }
    }

    private fun bodyWeightRecommendations(bodyWeights: List<BodyWeightLogEntry>): List<RecommendationItem> {
        if (bodyWeights.size < 7) {
            return listOf(
                RecommendationItem(
                    category = "Weight Trend",
                    message = "More data is needed for weight trends.",
                    rationale = "At least seven entries help smooth out daily fluctuations.",
                    severity = RecommendationSeverity.Info,
                    confidence = 0.8
                )
            )
        }

        val chronological = bodyWeights.asReversed().map { it.weightKg }
        val averages = bodyWeightTrendCalculator.movingAverage(chronological, 7)
        val delta = averages.last() - averages.first()
        return when {
            abs(delta) < 0.3 -> listOf(
                RecommendationItem(
                    category = "Weight Trend",
                    message = "Your smoothed weight trend is currently stable.",
                    rationale = "The change in the 7-day average is less than 0.3 kg.",
                    severity = RecommendationSeverity.Positive,
                    confidence = 0.65
                )
            )
            delta > 0.0 -> listOf(
                RecommendationItem(
                    category = "Weight Trend",
                    message = "Your smoothed weight is currently trending up.",
                    rationale = "Evaluate this in the context of your selected goal and several weeks of data.",
                    severity = RecommendationSeverity.Info,
                    confidence = 0.6
                )
            )
            else -> listOf(
                RecommendationItem(
                    category = "Weight Trend",
                    message = "Your smoothed weight is currently trending down.",
                    rationale = "Make sure your deficit, training, and recovery fit together.",
                    severity = RecommendationSeverity.Info,
                    confidence = 0.6
                )
            )
        }
    }
}
