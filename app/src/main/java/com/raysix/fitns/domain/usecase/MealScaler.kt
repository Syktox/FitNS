package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutritionFacts
import javax.inject.Inject

class MealScaler @Inject constructor() {
    fun scale(entry: FoodLogEntry, factor: Double, consumedAt: Long = System.currentTimeMillis()): FoodLogEntry {
        val safeFactor = factor.coerceAtLeast(0.0)
        return entry.copy(
            id = java.util.UUID.randomUUID().toString(),
            grams = entry.grams * safeFactor,
            nutrition = entry.nutrition.scale(safeFactor),
            consumedAt = consumedAt
        )
    }

    fun scale(entries: List<FoodLogEntry>, factor: Double, consumedAt: Long = System.currentTimeMillis()): List<FoodLogEntry> {
        return entries.map { scale(it, factor, consumedAt) }
    }

    private fun NutritionFacts.scale(factor: Double): NutritionFacts {
        return copy(
            caloriesKcal = caloriesKcal * factor,
            proteinGrams = proteinGrams * factor,
            carbohydratesGrams = carbohydratesGrams * factor,
            sugarGrams = sugarGrams * factor,
            fatGrams = fatGrams * factor,
            saturatedFatGrams = saturatedFatGrams * factor,
            fiberGrams = fiberGrams * factor,
            saltGrams = saltGrams * factor,
            sodiumMilligrams = sodiumMilligrams?.times(factor)
        )
    }
}
