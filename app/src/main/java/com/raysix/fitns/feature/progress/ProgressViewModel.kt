package com.raysix.fitns.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.EstimatedOneRepMaxCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ProgressUiState(
    val calories: List<TrendPoint> = emptyList(),
    val bodyWeight: List<TrendPoint> = emptyList(),
    val bodyWeightMovingAverage: List<TrendPoint> = emptyList(),
    val workoutVolume: List<TrendPoint> = emptyList(),
    val summary: ProgressSummary = ProgressSummary(),
    val bodyWeightAnalytics: BodyWeightAnalytics = BodyWeightAnalytics(),
    val strengthProgress: List<StrengthExerciseAnalytics> = emptyList(),
    val muscleGroupVolume: List<MuscleGroupVolumeAnalytics> = emptyList(),
    val nutritionAnalytics: List<NutritionAnalyticsWindow> = emptyList()
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

data class BodyWeightAnalytics(
    val latestWeightKg: Double? = null,
    val sevenDayAverageKg: Double? = null,
    val thirtyDayChangeKg: Double? = null,
    val distanceToTargetKg: Double? = null,
    val estimatedWeeklyRateKg: Double? = null
)

data class StrengthExerciseAnalytics(
    val exerciseName: String,
    val estimatedOneRepMax: List<TrendPoint>,
    val maxWeightKg: Double,
    val bestReps: Int,
    val volumeKg: Double,
    val workoutFrequency: Int,
    val recentSessions: List<String>
)

data class MuscleGroupVolumeAnalytics(
    val muscleGroup: String,
    val weeklySets: Int,
    val weeklyVolumeKg: Double,
    val changePercent: Double?
)

data class NutritionAnalyticsWindow(
    val days: Int,
    val loggedDays: Int,
    val averageCalories: Double,
    val averageProtein: Double,
    val averageCarbs: Double,
    val averageFat: Double,
    val averageFiber: Double,
    val calorieGoal: Double
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    nutritionRepository: NutritionRepository,
    bodyWeightRepository: BodyWeightRepository,
    workoutRepository: WorkoutRepository,
    profileRepository: ProfileRepository,
    private val estimatedOneRepMaxCalculator: EstimatedOneRepMaxCalculator
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = combine(
        nutritionRepository.observeFoodHistory(),
        bodyWeightRepository.observeHistory(),
        workoutRepository.observeHistory(),
        profileRepository.observeProfile(),
        profileRepository.observeNutritionGoal()
    ) { foods, weights, workouts, profile, nutritionGoal ->
        val calories = foods.toDailyCalories()
        val bodyWeight = weights.toBodyWeightTrend()
        val movingAverage = weights.toBodyWeightMovingAverage()
        val volume = workouts.toWorkoutVolume()
        ProgressUiState(
            calories = calories,
            bodyWeight = bodyWeight,
            bodyWeightMovingAverage = movingAverage,
            workoutVolume = volume,
            summary = ProgressSummary(
                averageCalories = calories.takeLast(7).takeIf { it.isNotEmpty() }?.map { it.value }?.average(),
                latestWeightKg = weights.firstOrNull()?.weightKg,
                weeklyVolumeKg = volume.takeLast(7).sumOf { it.value }
            ),
            bodyWeightAnalytics = weights.toBodyWeightAnalytics(profile.targetWeightKg),
            strengthProgress = workouts.toStrengthAnalytics(),
            muscleGroupVolume = workouts.toMuscleGroupVolumeAnalytics(),
            nutritionAnalytics = foods.toNutritionAnalytics(
                calorieGoal = nutritionGoal.caloriesKcal
            )
        )
    }.flowOn(Dispatchers.Default).stateIn(
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

    private fun List<BodyWeightLogEntry>.toBodyWeightMovingAverage(): List<TrendPoint> {
        val chronological = sortedBy { it.measuredAt }
        return chronological.mapIndexed { index, entry ->
            val startIndex = (index - 6).coerceAtLeast(0)
            val window = chronological.subList(startIndex, index + 1)
            TrendPoint(
                label = entry.measuredAt.toLocalDate().format(DayLabelFormatter),
                value = window.map { it.weightKg }.average()
            )
        }.takeLast(30)
    }

    private fun List<BodyWeightLogEntry>.toBodyWeightAnalytics(targetWeightKg: Double?): BodyWeightAnalytics {
        val chronological = sortedBy { it.measuredAt }
        val latest = chronological.lastOrNull() ?: return BodyWeightAnalytics()
        val latestDate = latest.measuredAt.toLocalDate()
        val thirtyDayReference = chronological
            .filter { it.measuredAt.toLocalDate() <= latestDate.minusDays(30) }
            .maxByOrNull { it.measuredAt }
        val first = chronological.firstOrNull()
        val elapsedDays = first?.let { (latest.measuredAt - it.measuredAt) / DayMillis.toDouble() } ?: 0.0
        val weeklyRate = if (first != null && elapsedDays >= 3.0) {
            (latest.weightKg - first.weightKg) / elapsedDays * 7.0
        } else {
            null
        }
        val sevenAverage = chronological.takeLast(7).map { it.weightKg }.takeIf { it.isNotEmpty() }?.average()
        return BodyWeightAnalytics(
            latestWeightKg = latest.weightKg,
            sevenDayAverageKg = sevenAverage,
            thirtyDayChangeKg = thirtyDayReference?.let { latest.weightKg - it.weightKg },
            distanceToTargetKg = targetWeightKg?.let { latest.weightKg - it },
            estimatedWeeklyRateKg = weeklyRate
        )
    }

    private fun List<WorkoutLogEntry>.toWorkoutVolume(): List<TrendPoint> {
        return lastDays(14).map { day ->
            TrendPoint(
                label = day.format(DayLabelFormatter),
                value = filter { it.loggedAt.toLocalDate() == day }.sumOf { it.volumeKg }
            )
        }
    }

    private fun List<WorkoutLogEntry>.toStrengthAnalytics(): List<StrengthExerciseAnalytics> {
        return groupBy { it.exercise.id }
            .values
            .mapNotNull { entries ->
                val exercise = entries.firstOrNull()?.exercise ?: return@mapNotNull null
                val sets = entries.flatMap { it.sets }
                if (sets.isEmpty()) return@mapNotNull null
                val oneRepMaxHistory = entries
                    .sortedBy { it.loggedAt }
                    .mapNotNull { entry ->
                        val best = entry.sets.maxOfOrNull {
                            estimatedOneRepMaxCalculator.calculate(it.weightKg, it.repetitions)
                        } ?: return@mapNotNull null
                        TrendPoint(entry.loggedAt.toLocalDate().format(DayLabelFormatter), best)
                    }
                    .takeLast(12)
                StrengthExerciseAnalytics(
                    exerciseName = exercise.name,
                    estimatedOneRepMax = oneRepMaxHistory,
                    maxWeightKg = sets.maxOf { it.weightKg },
                    bestReps = sets.maxOf { it.repetitions },
                    volumeKg = entries.sumOf { it.volumeKg },
                    workoutFrequency = entries.distinctBy { it.id }.size,
                    recentSessions = entries.sortedByDescending { it.loggedAt }
                        .take(3)
                        .map { "${it.loggedAt.toLocalDate().format(DayLabelFormatter)} | ${it.sets.sumOf { set -> set.sets }} sets" }
                )
            }
            .sortedByDescending { it.estimatedOneRepMax.lastOrNull()?.value ?: 0.0 }
            .take(8)
    }

    private fun List<WorkoutLogEntry>.toMuscleGroupVolumeAnalytics(): List<MuscleGroupVolumeAnalytics> {
        val today = LocalDate.now(ZoneId.systemDefault())
        val currentWeek = filter { it.loggedAt.toLocalDate() > today.minusDays(7) }
        val previousWeek = filter { entry ->
            val day = entry.loggedAt.toLocalDate()
            day <= today.minusDays(7) && day > today.minusDays(14)
        }
        return currentWeek
            .groupBy { it.exercise.muscleGroup }
            .map { (group, entries) ->
                val previousVolume = previousWeek
                    .filter { it.exercise.muscleGroup == group }
                    .sumOf { it.volumeKg }
                val currentVolume = entries.sumOf { it.volumeKg }
                MuscleGroupVolumeAnalytics(
                    muscleGroup = group,
                    weeklySets = entries.sumOf { it.sets.sumOf { set -> set.sets } },
                    weeklyVolumeKg = currentVolume,
                    changePercent = if (previousVolume > 0.0) ((currentVolume - previousVolume) / previousVolume) * 100.0 else null
                )
            }
            .sortedByDescending { it.weeklyVolumeKg }
    }

    private fun List<FoodLogEntry>.toNutritionAnalytics(calorieGoal: Double): List<NutritionAnalyticsWindow> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return listOf(7, 30, 90).mapNotNull { days ->
            val cutoff = today.minusDays(days.toLong() - 1)
            val entries = filter {
                val entryDate = it.consumedAt.toLocalDate()
                entryDate in cutoff..today
            }
            if (entries.isEmpty()) return@mapNotNull null

            val loggedDays = entries
                .map { it.consumedAt.toLocalDate() }
                .distinct()
                .size
            val dayCount = loggedDays.toDouble()
            val calories = entries.sumOf { it.nutrition.caloriesKcal } / dayCount
            NutritionAnalyticsWindow(
                days = days,
                loggedDays = loggedDays,
                averageCalories = calories,
                averageProtein = entries.sumOf { it.nutrition.proteinGrams } / dayCount,
                averageCarbs = entries.sumOf { it.nutrition.carbohydratesGrams } / dayCount,
                averageFat = entries.sumOf { it.nutrition.fatGrams } / dayCount,
                averageFiber = entries.sumOf { it.nutrition.fiberGrams } / dayCount,
                calorieGoal = calorieGoal
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
        const val DayMillis = 24L * 60L * 60L * 1000L
    }
}
