package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.TrainingLoadResult
import com.raysix.fitns.domain.model.WorkoutLogEntry
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class CalculateTrainingLoadUseCase @Inject constructor() {
    operator fun invoke(workouts: List<WorkoutLogEntry>, nowMillis: Long = System.currentTimeMillis()): TrainingLoadResult {
        val dayMillis = 24L * 60L * 60L * 1000L
        val weekAgo = nowMillis - 7L * dayMillis
        val todayStart = startOfLocalDayMillis(nowMillis)
        val week = workouts.filter { it.loggedAt >= weekAgo }
        val today = workouts.filter { it.loggedAt >= todayStart }
        val latest = workouts.maxByOrNull { it.loggedAt }
        return TrainingLoadResult(
            weeklySetCount = week.sumOf { entry -> entry.sets.sumOf { it.sets } },
            todaySetCount = today.sumOf { entry -> entry.sets.sumOf { it.sets } },
            weeklyVolumeKg = week.sumOf { it.volumeKg },
            daysSinceLastWorkout = latest?.let { ((nowMillis - it.loggedAt) / dayMillis).toInt().coerceAtLeast(0) }
        )
    }

    private fun startOfLocalDayMillis(nowMillis: Long): Long {
        return Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
