package com.raysix.fitns.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.CoachMetric
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.DailyCoachResult
import com.raysix.fitns.domain.model.NutrientAggregate
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.ReadinessResult
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.raysix.fitns.domain.usecase.CalculateDailyCoachUseCase
import com.raysix.fitns.domain.usecase.NutrientAggregator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val dashboard: DailyNutritionDashboard = EmptyDashboard,
    val workoutSummary: DashboardWorkoutSummary = DashboardWorkoutSummary(),
    val readiness: DashboardReadiness = DashboardReadiness(),
    val coach: DashboardCoach = EmptyCoach,
    val mealBreakdown: List<MealBreakdown> = emptyList(),
    val micronutrients: List<NutrientAggregate> = emptyList(),
    val message: String? = null
)

data class DashboardWorkoutSummary(
    val workoutCount: Int = 0,
    val setCount: Int = 0,
    val volumeKg: Double = 0.0,
    val latestExerciseName: String? = null
)

data class DashboardReadiness(
    val title: String = "No workout baseline",
    val summary: String = "Log workouts to get recovery guidance.",
    val status: String = "Baseline",
    val weeklySetCount: Int = 0,
    val daysSinceLastWorkout: Int? = null
)

data class DashboardCoach(
    val score: Int,
    val title: String,
    val summary: String,
    val focus: String,
    val metrics: List<DashboardGoalMetric>
)

data class DashboardGoalMetric(
    val label: String,
    val value: Double,
    val target: Double,
    val unit: String,
    val status: String
) {
    val progress: Float
        get() = if (target <= 0.0) 0f else (value / target).coerceIn(0.0, 1.25).toFloat()
}

data class MealBreakdown(
    val label: String,
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val entryCount: Int
)

private val EmptyCoach = DashboardCoach(
    score = 0,
    title = "Start logging",
    summary = "Log food, water, and workouts to unlock today's coaching.",
    focus = "Start with one meal and one hydration check-in.",
    metrics = emptyList()
)

private val EmptyDashboard = DailyNutritionDashboard(
    goal = NutritionGoal(
        caloriesKcal = 2300.0,
        proteinGrams = 150.0,
        carbohydrateGrams = 250.0,
        fatGrams = 75.0,
        fiberGrams = 30.0,
        waterMilliliters = 2500.0
    ),
    total = NutritionFacts(),
    waterMilliliters = 0.0,
    entries = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    workoutRepository: WorkoutRepository,
    profileRepository: ProfileRepository,
    aggregator: NutrientAggregator,
    private val calculateDailyCoach: CalculateDailyCoachUseCase
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        nutritionRepository.observeToday(),
        workoutRepository.observeHistory(),
        profileRepository.observeNutrientTargets(),
        message
    ) { dashboard, workouts, targets, message ->
        val workoutSummary = workouts.todaySummary()
        val coachResult = calculateDailyCoach(dashboard, workouts)
        DashboardUiState(
            dashboard = dashboard,
            workoutSummary = workoutSummary,
            readiness = coachResult.readiness.toUi(),
            coach = coachResult.toUi(),
            mealBreakdown = dashboard.toMealBreakdown(),
            micronutrients = aggregator.aggregate(dashboard.entries, targets)
                .filter { it.hasData && it.hasTarget }
                .sortedWith(compareBy<NutrientAggregate> { it.percent }.thenBy { it.key.label }),
            message = message
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    fun addWater(milliliters: Double) {
        viewModelScope.launch {
            val result = nutritionRepository.addWater(milliliters)
            message.value = when (result) {
                is AppResult.Success -> "Water logged."
                is AppResult.Failure -> "Water could not be logged."
            }
        }
    }

    fun removeWater(milliliters: Double) {
        viewModelScope.launch {
            val result = nutritionRepository.removeWater(milliliters)
            message.value = when (result) {
                is AppResult.Success -> "Water entry adjusted."
                is AppResult.Failure -> "Water could not be adjusted."
            }
        }
    }

    private fun List<com.raysix.fitns.domain.model.WorkoutLogEntry>.todaySummary(): DashboardWorkoutSummary {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val today = filter { it.loggedAt in start until end }
        return DashboardWorkoutSummary(
            workoutCount = today.distinctBy { it.id }.size,
            setCount = today.sumOf { it.sets.sumOf { set -> set.sets } },
            volumeKg = today.sumOf { it.volumeKg },
            latestExerciseName = today.maxByOrNull { it.loggedAt }?.exercise?.name
        )
    }

    private fun ReadinessResult.toUi(): DashboardReadiness {
        return DashboardReadiness(
            title = title,
            summary = summary,
            status = status,
            weeklySetCount = weeklySetCount,
            daysSinceLastWorkout = daysSinceLastWorkout
        )
    }

    private fun DailyCoachResult.toUi(): DashboardCoach {
        return DashboardCoach(
            score = score,
            title = title,
            summary = summary,
            focus = focus,
            metrics = metrics.map { it.toUi() }
        )
    }

    private fun CoachMetric.toUi(): DashboardGoalMetric {
        return DashboardGoalMetric(label = label, value = value, target = target, unit = unit, status = status)
    }

    private fun DailyNutritionDashboard.toMealBreakdown(): List<MealBreakdown> {
        return entries
            .groupBy { it.mealType.name }
            .map { (meal, items) ->
                MealBreakdown(
                    label = meal,
                    caloriesKcal = items.sumOf { it.nutrition.caloriesKcal },
                    proteinGrams = items.sumOf { it.nutrition.proteinGrams },
                    entryCount = items.size
                )
            }
            .sortedByDescending { it.caloriesKcal }
    }

}
