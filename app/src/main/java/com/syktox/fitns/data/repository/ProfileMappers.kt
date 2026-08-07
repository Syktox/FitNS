package com.syktox.fitns.data.repository

import com.syktox.fitns.data.local.entity.NutrientTargetEntity
import com.syktox.fitns.data.local.entity.NutritionGoalEntity
import com.syktox.fitns.data.local.entity.SyncStatus
import com.syktox.fitns.data.local.entity.UserProfileEntity
import com.syktox.fitns.domain.model.DefaultUserProfileId
import com.syktox.fitns.domain.model.NutrientKey
import com.syktox.fitns.domain.model.NutrientTarget
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.model.UserProfile
import java.util.UUID

val DefaultNutritionGoal = NutritionGoal(
    caloriesKcal = 2300.0,
    proteinGrams = 150.0,
    carbohydrateGrams = 250.0,
    fatGrams = 75.0,
    fiberGrams = 30.0,
    waterMilliliters = 2500.0
)

fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        age = age,
        sexOrPhysiology = sexOrPhysiology,
        heightCm = heightCm,
        weightKg = weightKg,
        targetWeightKg = targetWeightKg,
        activityLevel = activityLevel,
        trainingDaysPerWeek = trainingDaysPerWeek,
        goal = goal,
        dietStyle = dietStyle,
        allergies = allergies
    )
}

fun UserProfile.toEntity(now: Long = System.currentTimeMillis()): UserProfileEntity {
    return UserProfileEntity(
        id = id.ifBlank { DefaultUserProfileId },
        age = age,
        sexOrPhysiology = sexOrPhysiology,
        heightCm = heightCm,
        weightKg = weightKg,
        targetWeightKg = targetWeightKg,
        activityLevel = activityLevel.ifBlank { "Moderate" },
        trainingDaysPerWeek = trainingDaysPerWeek.coerceAtLeast(0),
        goal = goal.ifBlank { "Maintain" },
        dietStyle = dietStyle,
        allergies = allergies,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun NutritionGoalEntity.toDomain(): NutritionGoal {
    return NutritionGoal(
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbohydrateGrams = carbohydrateGrams,
        fatGrams = fatGrams,
        fiberGrams = fiberGrams,
        waterMilliliters = waterMilliliters
    )
}

fun NutritionGoal.toVersionedEntity(now: Long = System.currentTimeMillis()): NutritionGoalEntity {
    return NutritionGoalEntity(
        id = UUID.randomUUID().toString(),
        userProfileId = DefaultUserProfileId,
        caloriesKcal = caloriesKcal,
        proteinGrams = proteinGrams,
        carbohydrateGrams = carbohydrateGrams,
        fatGrams = fatGrams,
        fiberGrams = fiberGrams,
        waterMilliliters = waterMilliliters,
        validFrom = now,
        validTo = null,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

fun NutrientTargetEntity.toDomain(): NutrientTarget {
    return NutrientTarget(
        key = NutrientKey.entries.firstOrNull { it.name == nutrientKey } ?: NutrientKey.Calcium,
        targetAmount = targetAmount,
        unit = unit,
        source = source
    )
}

fun NutrientTarget.toEntity(now: Long = System.currentTimeMillis()): NutrientTargetEntity {
    return NutrientTargetEntity(
        id = UUID.randomUUID().toString(),
        userProfileId = DefaultUserProfileId,
        nutrientKey = key.name,
        targetAmount = targetAmount,
        unit = unit,
        source = source,
        validFrom = now,
        validTo = null,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}
