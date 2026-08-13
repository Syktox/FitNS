package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ReadinessResult
import com.raysix.fitns.domain.model.TrainingLoadResult
import javax.inject.Inject

class CalculateReadinessUseCase @Inject constructor() {
    operator fun invoke(load: TrainingLoadResult): ReadinessResult {
        val status = when {
            load.daysSinceLastWorkout == null -> "Baseline"
            load.daysSinceLastWorkout == 0 && load.weeklySetCount >= 18 -> "Recover"
            load.daysSinceLastWorkout == 0 -> "Logged"
            load.weeklySetCount >= 24 -> "Deload"
            load.daysSinceLastWorkout >= 2 && load.weeklySetCount < 18 -> "High"
            else -> "Ready"
        }
        val title = when (status) {
            "Baseline" -> "No workout baseline"
            "Recover" -> "Recovery priority"
            "Logged" -> "Workout logged today"
            "Deload" -> "High weekly load"
            "High" -> "Readiness high"
            else -> "Ready to train"
        }
        val summary = when (status) {
            "Baseline" -> "Log workouts to get recovery guidance."
            "Recover" -> "You already trained today and weekly volume is high. Keep the next session light."
            "Logged" -> "Workout is logged for today. Add more only if recovery still feels good."
            "Deload" -> "Weekly set count is elevated. Consider fewer hard sets or easier loads."
            "High" -> "You have not trained for ${load.daysSinceLastWorkout} days and weekly volume is below your recent target range."
            else -> "No heavy recent load detected. A planned strength session fits today."
        }
        return ReadinessResult(
            title = title,
            summary = summary,
            status = status,
            weeklySetCount = load.weeklySetCount,
            daysSinceLastWorkout = load.daysSinceLastWorkout
        )
    }
}
