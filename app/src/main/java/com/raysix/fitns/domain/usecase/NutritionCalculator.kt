package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutritionFacts

class NutritionCalculator {
    fun scalePer100g(per100g: NutritionFacts, grams: Double): NutritionFacts {
        require(grams >= 0.0) { "Grams must not be negative." }
        val factor = grams / 100.0
        return NutritionFacts(
            caloriesKcal = per100g.caloriesKcal * factor,
            proteinGrams = per100g.proteinGrams * factor,
            carbohydratesGrams = per100g.carbohydratesGrams * factor,
            sugarGrams = per100g.sugarGrams * factor,
            fatGrams = per100g.fatGrams * factor,
            saturatedFatGrams = per100g.saturatedFatGrams * factor,
            fiberGrams = per100g.fiberGrams * factor,
            saltGrams = per100g.saltGrams * factor,
            sodiumMilligrams = per100g.sodiumMilligrams?.times(factor)
        )
    }

    fun summarize(entries: List<FoodLogEntry>): NutritionFacts {
        return entries.fold(NutritionFacts()) { total, entry ->
            total + entry.nutrition
        }
    }
}

operator fun NutritionFacts.plus(other: NutritionFacts): NutritionFacts {
    return NutritionFacts(
        caloriesKcal = caloriesKcal + other.caloriesKcal,
        proteinGrams = proteinGrams + other.proteinGrams,
        carbohydratesGrams = carbohydratesGrams + other.carbohydratesGrams,
        sugarGrams = sugarGrams + other.sugarGrams,
        fatGrams = fatGrams + other.fatGrams,
        saturatedFatGrams = saturatedFatGrams + other.saturatedFatGrams,
        fiberGrams = fiberGrams + other.fiberGrams,
        saltGrams = saltGrams + other.saltGrams,
        sodiumMilligrams = nullableSum(sodiumMilligrams, other.sodiumMilligrams)
    )
}

private fun nullableSum(left: Double?, right: Double?): Double? {
    if (left == null && right == null) return null
    return (left ?: 0.0) + (right ?: 0.0)
}

