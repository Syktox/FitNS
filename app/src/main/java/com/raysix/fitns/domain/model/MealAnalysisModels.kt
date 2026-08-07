package com.raysix.fitns.domain.model

data class MealAnalysisItem(
    val name: String,
    val estimatedGrams: Double,
    val confidence: Double,
    val nutrition: NutritionFacts
)

data class MealAnalysisResult(
    val items: List<MealAnalysisItem>,
    val total: NutritionFacts,
    val disclaimer: String
)
