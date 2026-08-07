package com.syktox.fitns.domain.model

import java.util.UUID

enum class MealType {
    Breakfast,
    Lunch,
    Dinner,
    Snack,
    Custom
}

enum class DataQuality {
    Verified,
    Estimated,
    Missing
}

data class NutritionFacts(
    val caloriesKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbohydratesGrams: Double = 0.0,
    val sugarGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val saturatedFatGrams: Double = 0.0,
    val fiberGrams: Double = 0.0,
    val saltGrams: Double = 0.0,
    val sodiumMilligrams: Double? = null
)

data class FoodLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String?,
    val mealType: MealType,
    val grams: Double,
    val nutrition: NutritionFacts,
    val dataQuality: DataQuality = DataQuality.Verified,
    val notes: String = "",
    val consumedAt: Long = System.currentTimeMillis(),
    val micronutrients: Micronutrients = Micronutrients()
)

data class FoodProductLookup(
    val barcode: String?,
    val name: String,
    val brand: String?,
    val servingSizeGrams: Double?,
    val nutritionPer100g: NutritionFacts
)

data class FoodFavoritePreset(
    val id: String,
    val name: String,
    val brand: String?,
    val servingSizeGrams: Double,
    val nutritionPer100g: NutritionFacts,
    val notes: String = ""
)

data class NutritionGoal(
    val caloriesKcal: Double,
    val proteinGrams: Double,
    val carbohydrateGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double,
    val waterMilliliters: Double
)

data class VersionedNutritionGoal(
    val goal: NutritionGoal,
    val validFrom: Long
)

data class UserProfile(
    val id: String = DefaultUserProfileId,
    val age: Int? = null,
    val sexOrPhysiology: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val activityLevel: String = "Moderate",
    val trainingDaysPerWeek: Int = 3,
    val goal: String = "Maintain",
    val dietStyle: String? = null,
    val allergies: String? = null
)

const val DefaultUserProfileId = "default-user"

data class NutrientProgress(
    val label: String,
    val consumed: Double?,
    val target: Double,
    val unit: String,
    val dataQuality: DataQuality
) {
    val percent: Float
        get() = if (consumed == null || target <= 0.0) 0f else (consumed / target).coerceIn(0.0, 1.0).toFloat()

    val remaining: Double?
        get() = consumed?.let { (target - it).coerceAtLeast(0.0) }
}

data class DailyNutritionDashboard(
    val goal: NutritionGoal,
    val total: NutritionFacts,
    val waterMilliliters: Double,
    val entries: List<FoodLogEntry>
) {
    val remainingCalories: Double
        get() = (goal.caloriesKcal - total.caloriesKcal).coerceAtLeast(0.0)
}
