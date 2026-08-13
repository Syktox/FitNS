package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.MealType
import com.raysix.fitns.domain.model.NutritionFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MealScalerTest {
    private val scaler = MealScaler()

    @Test
    fun scale_multipliesGramsAndNutrition() {
        val entry = FoodLogEntry(
            id = "entry-1",
            name = "Chicken Rice Bowl",
            brand = null,
            mealType = MealType.Lunch,
            grams = 400.0,
            nutrition = NutritionFacts(
                caloriesKcal = 600.0,
                proteinGrams = 45.0,
                carbohydratesGrams = 70.0,
                sugarGrams = 4.0,
                fatGrams = 18.0,
                saturatedFatGrams = 3.0,
                fiberGrams = 8.0,
                saltGrams = 1.2,
                sodiumMilligrams = 500.0
            ),
            consumedAt = 1L
        )

        val scaled = scaler.scale(entry, 1.5, consumedAt = 2L)

        assertNotEquals(entry.id, scaled.id)
        assertEquals(600.0, scaled.grams, 0.001)
        assertEquals(900.0, scaled.nutrition.caloriesKcal, 0.001)
        assertEquals(67.5, scaled.nutrition.proteinGrams, 0.001)
        assertEquals(750.0, scaled.nutrition.sodiumMilligrams ?: 0.0, 0.001)
        assertEquals(2L, scaled.consumedAt)
    }

    @Test
    fun scale_clampsNegativeFactorToZero() {
        val entry = FoodLogEntry(
            name = "Rice",
            brand = null,
            mealType = MealType.Lunch,
            grams = 100.0,
            nutrition = NutritionFacts(caloriesKcal = 130.0)
        )

        val scaled = scaler.scale(entry, -1.0)

        assertEquals(0.0, scaled.grams, 0.001)
        assertEquals(0.0, scaled.nutrition.caloriesKcal, 0.001)
    }
}
