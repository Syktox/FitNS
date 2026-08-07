package com.raysix.fitns.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.NutrientAggregate
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
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
    aggregator: NutrientAggregator
) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        nutritionRepository.observeToday(),
        workoutRepository.observeHistory(),
        profileRepository.observeNutrientTargets(),
        message
    ) { dashboard, workouts, targets, message ->
        val workoutSummary = workouts.todaySummary()
        DashboardUiState(
            dashboard = dashboard,
            workoutSummary = workoutSummary,
            readiness = workouts.toReadiness(),
            coach = dashboard.toCoach(workoutSummary),
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

    private fun List<com.raysix.fitns.domain.model.WorkoutLogEntry>.toReadiness(): DashboardReadiness {
        if (isEmpty()) return DashboardReadiness()
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L
        val weekAgo = now - 7L * dayMillis
        val weeklySetCount = filter { it.loggedAt >= weekAgo }.sumOf { entry -> entry.sets.sumOf { it.sets } }
        val latest = maxByOrNull { it.loggedAt }
        val daysSinceLastWorkout = latest?.let { ((now - it.loggedAt) / dayMillis).toInt().coerceAtLeast(0) }

        val status = when {
            daysSinceLastWorkout == 0 && weeklySetCount >= 18 -> "Recover"
            daysSinceLastWorkout == 0 -> "Logged"
            daysSinceLastWorkout != null && daysSinceLastWorkout >= 3 -> "Ready"
            weeklySetCount >= 24 -> "Deload"
            else -> "Ready"
        }
        val title = when (status) {
            "Recover" -> "Recovery priority"
            "Logged" -> "Workout logged today"
            "Deload" -> "High weekly load"
            else -> "Ready to train"
        }
        val summary = when (status) {
            "Recover" -> "You already trained today and weekly volume is high. Keep the next session light."
            "Logged" -> "Workout is logged for today. Add more only if recovery still feels good."
            "Deload" -> "Weekly set count is elevated. Consider fewer hard sets or easier loads."
            else -> "No heavy recent load detected. A planned strength session fits today."
        }

        return DashboardReadiness(
            title = title,
            summary = summary,
            status = status,
            weeklySetCount = weeklySetCount,
            daysSinceLastWorkout = daysSinceLastWorkout
        )
    }

    private fun DailyNutritionDashboard.toCoach(workoutSummary: DashboardWorkoutSummary): DashboardCoach {
        val calorieRatio = total.caloriesKcal.ratioTo(goal.caloriesKcal)
        val proteinRatio = total.proteinGrams.ratioTo(goal.proteinGrams)
        val waterRatio = waterMilliliters.ratioTo(goal.waterMilliliters)
        val trainingScore = if (workoutSummary.setCount > 0) 1.0 else 0.0
        val nutritionScore = listOf(
            calorieRatio.scoreForRange(0.75, 1.08),
            proteinRatio.scoreForMinimum(0.75),
            waterRatio.scoreForMinimum(0.7)
        ).average()
        val score = ((nutritionScore * 0.75 + trainingScore * 0.25) * 100).toInt().coerceIn(0, 100)
        val metrics = listOf(
            DashboardGoalMetric("Calories", total.caloriesKcal, goal.caloriesKcal, "kcal", calorieRatio.statusForCalories()),
            DashboardGoalMetric("Protein", total.proteinGrams, goal.proteinGrams, "g", proteinRatio.statusForMinimum()),
            DashboardGoalMetric("Water", waterMilliliters, goal.waterMilliliters, "ml", waterRatio.statusForMinimum()),
            DashboardGoalMetric("Workout", workoutSummary.setCount.toDouble(), 1.0, "sets", if (workoutSummary.setCount > 0) "Done" else "Open")
        )
        val focus = when {
            entries.isEmpty() -> "Log your first meal so the targets become actionable."
            proteinRatio < 0.65 -> "Prioritize protein at the next meal."
            waterRatio < 0.6 -> "Add water now and check hydration again later."
            calorieRatio > 1.1 -> "Keep the rest of the day lighter and protein-forward."
            workoutSummary.setCount == 0 -> "Add a short strength session or log today's completed sets."
            else -> "Stay consistent and finish the day close to target."
        }
        val title = when {
            score >= 85 -> "Strong day"
            score >= 65 -> "On track"
            score >= 35 -> "Needs attention"
            else -> "Just getting started"
        }
        val summary = "${entries.size} foods, ${workoutSummary.setCount} sets, ${waterMilliliters.toInt()} ml water logged."
        return DashboardCoach(score = score, title = title, summary = summary, focus = focus, metrics = metrics)
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

    private fun Double.ratioTo(target: Double): Double {
        return if (target <= 0.0) 0.0 else this / target
    }

    private fun Double.scoreForMinimum(minimum: Double): Double {
        return (this / minimum).coerceIn(0.0, 1.0)
    }

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
