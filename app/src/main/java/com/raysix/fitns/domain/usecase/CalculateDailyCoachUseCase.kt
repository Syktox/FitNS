package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.DailyCoachResult
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.WorkoutLogEntry
import javax.inject.Inject

class CalculateDailyCoachUseCase @Inject constructor(
    private val calculateTrainingLoad: CalculateTrainingLoadUseCase,
    private val calculateReadiness: CalculateReadinessUseCase,
    private val calculateNutritionAdherence: CalculateNutritionAdherenceUseCase
) {
    operator fun invoke(
        dashboard: DailyNutritionDashboard,
        workouts: List<WorkoutLogEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): DailyCoachResult {
        val trainingLoad = calculateTrainingLoad(workouts, nowMillis)
        val readiness = calculateReadiness(trainingLoad)
        val adherence = calculateNutritionAdherence(dashboard, trainingLoad.todaySetCount)
        val trainingScore = if (trainingLoad.todaySetCount > 0) 1.0 else 0.0
        val score = ((adherence.score * 0.75 + trainingScore * 0.25) * 100).toInt().coerceIn(0, 100)
        val title = when {
            score >= 85 -> "Strong day"
            score >= 65 -> "On track"
            score >= 35 -> "Needs attention"
            else -> "Just getting started"
        }
        val summary = "${dashboard.entries.size} foods, ${trainingLoad.todaySetCount} sets, ${dashboard.waterMilliliters.toInt()} ml water logged."
        return DailyCoachResult(
            score = score,
            title = title,
            summary = summary,
            focus = adherence.focus,
            metrics = adherence.metrics,
            readiness = readiness
        )
    }
}
