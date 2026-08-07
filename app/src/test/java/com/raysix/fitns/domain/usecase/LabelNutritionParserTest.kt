package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.NutrientKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelNutritionParserTest {

    private val parser = LabelNutritionParser()

    @Test
    fun `parses german label per 100 g`() {
        val result = parser.parse(
            """
            Müsli Hafer
            Nährwerte pro 100 g
            Brennwert 380 kcal
            Protein 12,5 g
            Kohlenhydrate 65 g
            davon Zucker 20 g
            Fett 8 g
            davon gesättigte Fettsäuren 1,5 g
            Ballaststoffe 9 g
            Salz 0,4 g
            """.trimIndent()
        )

        assertEquals("Müsli Hafer", result.detectedName)
        assertFalse(result.perPortion)
        assertEquals(380.0, result.nutrition.caloriesKcal, 0.001)
        assertEquals(12.5, result.nutrition.proteinGrams, 0.001)
        assertEquals(65.0, result.nutrition.carbohydratesGrams, 0.001)
        assertEquals(20.0, result.nutrition.sugarGrams, 0.001)
        assertEquals(8.0, result.nutrition.fatGrams, 0.001)
        assertEquals(1.5, result.nutrition.saturatedFatGrams, 0.001)
        assertEquals(9.0, result.nutrition.fiberGrams, 0.001)
        assertEquals(0.4, result.nutrition.saltGrams, 0.001)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `parses english label and sodium`() {
        val result = parser.parse(
            """
            Chocolate Bar
            Nutrition Facts per 100g
            Energy 500 kcal
            Protein 6 g
            Carbohydrates 58 g
            of which sugars 34 g
            Fat 26 g
            of which saturates 15 g
            Fibre 3 g
            Salt 0.2 g
            Sodium 80 mg
            """.trimIndent()
        )

        assertEquals("Chocolate Bar", result.detectedName)
        assertEquals(500.0, result.nutrition.caloriesKcal, 0.001)
        assertEquals(34.0, result.nutrition.sugarGrams, 0.001)
        assertEquals(80.0, result.nutrition.sodiumMilligrams ?: 0.0, 0.001)
    }

    @Test
    fun `detects per portion table`() {
        val result = parser.parse(
            """
            Müsli
            Nährwerte pro Portion
            Energie 220 kcal
            Protein 5 g
            Kohlenhydrate 30 g
            Fett 4 g
            """.trimIndent()
        )

        assertTrue(result.perPortion)
    }

    @Test
    fun `extracts micronutrients`() {
        val result = parser.parse(
            """
            Protein Shake
            Nährwerte pro 100 ml
            Energie 45 kcal
            Protein 3,5 g
            Kohlenhydrate 4 g
            Fett 0,5 g
            Calcium 120 mg
            Magnesium 30 mg
            Vitamin C 15 mg
            """.trimIndent()
        )

        assertNotNull(result.micronutrients.values[NutrientKey.Calcium])
        assertEquals(120.0, result.micronutrients.values[NutrientKey.Calcium]!!.amount, 0.001)
        assertEquals(30.0, result.micronutrients.values[NutrientKey.Magnesium]!!.amount, 0.001)
        assertEquals(15.0, result.micronutrients.values[NutrientKey.VitaminC]!!.amount, 0.001)
        assertTrue(result.perPortion)
    }

    @Test
    fun `reports warnings when macros are missing`() {
        val result = parser.parse(
            """
            Unbekanntes Produkt
            Nährwerte pro 100 g
            Kohlenhydrate 30 g
            """.trimIndent()
        )

        assertFalse(result.warnings.isEmpty())
        assertTrue(result.warnings.any { it.contains("calorie", ignoreCase = true) })
        assertTrue(result.warnings.any { it.contains("protein", ignoreCase = true) })
        assertTrue(result.warnings.any { it.contains("fat", ignoreCase = true) })
    }

    @Test
    fun `converts kilojoules to kilocalories`() {
        val result = parser.parse(
            """
            Getränk
            Nährwerte pro 100 ml
            Brennwert 167 kJ
            Protein 0 g
            Kohlenhydrate 9,6 g
            Fett 0 g
            """.trimIndent()
        )

        assertEquals(167.0 / 4.184, result.nutrition.caloriesKcal, 1.0)
    }
}
