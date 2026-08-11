package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.DailyNutritionDashboard
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutritionFacts
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateDailyCoachUseCaseTest {
    private val useCase = CalculateDailyCoachUseCase(
        calculateTrainingLoad = CalculateTrainingLoadUseCase(),
        calculateReadiness = CalculateReadinessUseCase(),
        calculateNutritionAdherence = CalculateNutritionAdherenceUseCase()
    )

    @Test
    fun invoke_explainsHighReadinessWhenRestedAndWeeklyLoadLow() {
        val now = 10L * DayMillis
        val workout = WorkoutLogEntry(
            exercise = Exercise(
                id = "row",
                name = "Row",
                muscleGroup = "Back",
                machineType = "Machine"
            ),
            sets = listOf(WorkoutSetInput(weightKg = 60.0, repetitions = 10, rpe = 8, sets = 3)),
            loggedAt = now - 2L * DayMillis
        )

        val result = useCase(emptyDashboard(), listOf(workout), nowMillis = now)

        assertEquals("High", result.readiness.status)
        assertTrue(result.readiness.summary.contains("2 days"))
    }

    @Test
    fun invoke_prioritizesProteinWhenProteinIsLow() {
        val dashboard = emptyDashboard().copy(
            entries = listOf(
                FoodLogEntry(
                    name = "Toast",
                    brand = null,
                    mealType = MealType.Breakfast,
                    grams = 100.0,
                    nutrition = NutritionFacts(caloriesKcal = 300.0, proteinGrams = 10.0)
                )
            ),
            total = NutritionFacts(caloriesKcal = 300.0, proteinGrams = 10.0),
            waterMilliliters = 1500.0
        )

        val result = useCase(dashboard, emptyList(), nowMillis = 10L * DayMillis)

        assertEquals("Prioritize protein at the next meal.", result.focus)
        assertTrue(result.metrics.any { it.label == "Protein" && it.status == "Open" })
    }

    private fun emptyDashboard(): DailyNutritionDashboard {
        return DailyNutritionDashboard(
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
    }

    private companion object {
        const val DayMillis = 24L * 60L * 60L * 1000L
    }
}
