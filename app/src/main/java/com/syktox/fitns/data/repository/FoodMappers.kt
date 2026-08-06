package com.syktox.fitns.data.repository

import com.syktox.fitns.data.local.entity.FoodEntryEntity
import com.syktox.fitns.data.local.entity.SyncStatus
import com.syktox.fitns.domain.model.DataQuality
import com.syktox.fitns.domain.model.FoodLogEntry
import com.syktox.fitns.domain.model.MealType
import com.syktox.fitns.domain.model.NutritionFacts

fun FoodEntryEntity.toDomain(): FoodLogEntry {
    return FoodLogEntry(
        id = id,
        name = name,
        brand = brand,
        mealType = mealType.toMealType(),
        grams = grams,
        nutrition = NutritionFacts(
            caloriesKcal = caloriesKcal,
            proteinGrams = proteinGrams,
            carbohydratesGrams = carbohydratesGrams,
            sugarGrams = sugarGrams,
            fatGrams = fatGrams,
            saturatedFatGrams = saturatedFatGrams,
            fiberGrams = fiberGrams,
            saltGrams = saltGrams,
            sodiumMilligrams = sodiumMilligrams
        ),
        dataQuality = dataQuality.toDataQuality(),
        notes = notes,
        consumedAt = consumedAt
    )
}

fun FoodLogEntry.toEntity(now: Long = System.currentTimeMillis()): FoodEntryEntity {
    return FoodEntryEntity(
        id = id,
        mealId = null,
        foodProductId = null,
        name = name,
        brand = brand,
        mealType = mealType.name,
        grams = grams,
        caloriesKcal = nutrition.caloriesKcal,
        proteinGrams = nutrition.proteinGrams,
        carbohydratesGrams = nutrition.carbohydratesGrams,
        sugarGrams = nutrition.sugarGrams,
        fatGrams = nutrition.fatGrams,
        saturatedFatGrams = nutrition.saturatedFatGrams,
        fiberGrams = nutrition.fiberGrams,
        saltGrams = nutrition.saltGrams,
        sodiumMilligrams = nutrition.sodiumMilligrams,
        consumedAt = this.consumedAt,
        notes = notes,
        dataQuality = dataQuality.name,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

private fun String.toMealType(): MealType {
    return MealType.entries.firstOrNull { it.name == this } ?: MealType.Custom
}

private fun String.toDataQuality(): DataQuality {
    return DataQuality.entries.firstOrNull { it.name == this } ?: DataQuality.Missing
}
