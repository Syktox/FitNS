package com.raysix.fitns.feature.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealAnalysisDraftHelpersTest {

    @Test
    fun `totals sum valid draft values including comma decimals`() {
        val totals = calculateMealAnalysisTotals(
            listOf(
                validItem(
                    id = "first",
                    calories = "120,5",
                    protein = "10,25",
                    carbs = "15,5",
                    fat = "4,75"
                ),
                validItem(
                    id = "second",
                    calories = "79.5",
                    protein = "9.75",
                    carbs = "4.5",
                    fat = "5.25"
                )
            )
        )

        assertEquals(200.0, totals.calories, 0.001)
        assertEquals(20.0, totals.protein, 0.001)
        assertEquals(20.0, totals.carbohydrates, 0.001)
        assertEquals(10.0, totals.fat, 0.001)
    }

    @Test
    fun `empty draft has zero totals`() {
        assertEquals(MealAnalysisTotals(), calculateMealAnalysisTotals(emptyList()))
    }

    @Test
    fun `valid item accepts comma decimals and zero nutrition`() {
        val item = validItem(
            grams = "125,5",
            calories = "0",
            protein = "0,0",
            carbs = "12,5",
            fat = "3.25"
        )

        assertTrue(item.isValid())
    }

    @Test
    fun `item is invalid when required values are blank negative or grams are nonpositive`() {
        val invalidItems = listOf(
            validItem(id = "blank-name", name = "   "),
            validItem(id = "blank-grams", grams = ""),
            validItem(id = "blank-calories", calories = ""),
            validItem(id = "blank-protein", protein = ""),
            validItem(id = "blank-carbs", carbs = ""),
            validItem(id = "blank-fat", fat = ""),
            validItem(id = "negative-grams", grams = "-1"),
            validItem(id = "zero-grams", grams = "0"),
            validItem(id = "negative-calories", calories = "-1"),
            validItem(id = "negative-protein", protein = "-1"),
            validItem(id = "negative-carbs", carbs = "-1"),
            validItem(id = "negative-fat", fat = "-1")
        )

        invalidItems.forEach { item ->
            assertFalse("Expected ${item.id} to be invalid", item.isValid())
        }
    }

    private fun validItem(
        id: String = "item",
        name: String = "Rice bowl",
        grams: String = "250",
        calories: String = "420",
        protein: String = "24",
        carbs: String = "55",
        fat: String = "12"
    ) = EditableMealItem(
        id = id,
        name = name,
        grams = grams,
        confidence = 0.9,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat
    )
}
