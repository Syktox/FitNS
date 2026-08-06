package com.syktox.fitns.domain.usecase

import com.syktox.fitns.domain.model.NutritionFacts
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {
    private val calculator = NutritionCalculator()

    @Test
    fun scalePer100g_scalesValuesByServingSize() {
        val result = calculator.scalePer100g(
            per100g = NutritionFacts(
                caloriesKcal = 250.0,
                proteinGrams = 12.0,
                carbohydratesGrams = 30.0,
                fatGrams = 8.0,
                fiberGrams = 4.0
            ),
            grams = 150.0
        )

        assertEquals(375.0, result.caloriesKcal, 0.001)
        assertEquals(18.0, result.proteinGrams, 0.001)
        assertEquals(45.0, result.carbohydratesGrams, 0.001)
        assertEquals(12.0, result.fatGrams, 0.001)
        assertEquals(6.0, result.fiberGrams, 0.001)
    }
}

