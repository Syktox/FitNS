package com.syktox.fitns.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syktox.fitns.domain.model.BodyWeightLogEntry
import com.syktox.fitns.domain.model.FoodLogEntry
import com.syktox.fitns.domain.model.WorkoutLogEntry
import com.syktox.fitns.domain.repository.BodyWeightRepository
import com.syktox.fitns.domain.repository.NutritionRepository
import com.syktox.fitns.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProgressUiState(
    val calories: List<TrendPoint> = emptyList(),
    val bodyWeight: List<TrendPoint> = emptyList(),
    val workoutVolume: List<TrendPoint> = emptyList(),
    val summary: ProgressSummary = ProgressSummary()
)

data class TrendPoint(
    val label: String,
    val value: Double
)

data class ProgressSummary(
    val averageCalories: Double? = null,
    val latestWeightKg: Double? = null,
    val weeklyVolumeKg: Double = 0.0
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    nutritionRepository: NutritionRepository,
    bodyWeightRepository: BodyWeightRepository,
    workoutRepository: WorkoutRepository
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = combine(
        nutritionRepository.observeFoodHistory(),
        bodyWeightRepository.observeHistory(),
        workoutRepository.observeHistory()
    ) { foods, weights, workouts ->
        val calories = foods.toDailyCalories()
        val bodyWeight = weights.toBodyWeightTrend()
        val volume = workouts.toWorkoutVolume()
        ProgressUiState(
            calories = calories,
            bodyWeight = bodyWeight,
            workoutVolume = volume,
            summary = ProgressSummary(
                averageCalories = calories.takeLast(7).takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
                latestWeightKg = weights.firstOrNull()?.weightKg,
                weeklyVolumeKg = volume.takeLast(7).sumOf { it.value }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    private fun List<FoodLogEntry>.toDailyCalories(): List<TrendPoint> {
        return lastDays(14).map { day ->
            TrendPoint(
                label = day.format(DayLabelFormatter),
                value = filter { it.consumedAt.toLocalDate() == day }.sumOf { it.nutrition.caloriesKcal }
            )
        }
    }

    private fun List<BodyWeightLogEntry>.toBodyWeightTrend(): List<TrendPoint> {
        return sortedBy { it.measuredAt }
            .takeLast(14)
            .map {
                TrendPoint(
                    label = it.measuredAt.toLocalDate().format(DayLabelFormatter),
                    value = it.weightKg
                )
            }
    }

    private fun List<WorkoutLogEntry>.toWorkoutVolume(): List<TrendPoint> {
        return lastDays(14).map { day ->
            TrendPoint(
                label = day.format(DayLabelFormatter),
                value = filter { it.loggedAt.toLocalDate() == day }.sumOf { it.volumeKg }
            )
        }
    }

    private fun lastDays(count: Long): List<LocalDate> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return (count - 1 downTo 0).map { today.minusDays(it) }
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private companion object {
        val DayLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd")
    }
}
