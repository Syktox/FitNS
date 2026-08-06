package com.syktox.fitns.data.repository

import com.syktox.fitns.core.model.AppError
import com.syktox.fitns.core.model.AppResult
import com.syktox.fitns.data.local.dao.ProfileDao
import com.syktox.fitns.domain.model.DefaultUserProfileId
import com.syktox.fitns.domain.model.NutritionGoal
import com.syktox.fitns.domain.model.UserProfile
import com.syktox.fitns.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) : ProfileRepository {
    override fun observeProfile(): Flow<UserProfile> {
        return profileDao.observeProfile(DefaultUserProfileId).map { entity ->
            entity?.toDomain() ?: UserProfile()
        }
    }

    override fun observeNutritionGoal(): Flow<NutritionGoal> {
        return profileDao.observeLatestNutritionGoal(DefaultUserProfileId).map { entity ->
            entity?.toDomain() ?: DefaultNutritionGoal
        }
    }

    override suspend fun saveProfile(profile: UserProfile): AppResult<Unit> {
        val error = validateProfile(profile)
        if (error != null) return AppResult.Failure(error)
        profileDao.upsertProfile(profile.copy(id = DefaultUserProfileId).toEntity())
        return AppResult.Success(Unit)
    }

    override suspend fun saveNutritionGoal(goal: NutritionGoal): AppResult<Unit> {
        val error = validateGoal(goal)
        if (error != null) return AppResult.Failure(error)
        profileDao.upsertNutritionGoal(goal.toEntity())
        return AppResult.Success(Unit)
    }

    private fun validateProfile(profile: UserProfile): AppError? {
        return when {
            profile.age != null && profile.age !in 1..120 -> AppError.Validation("Age looks implausible.")
            profile.heightCm != null && profile.heightCm !in 50.0..260.0 -> AppError.Validation("Height looks implausible.")
            profile.weightKg != null && profile.weightKg !in 20.0..500.0 -> AppError.Validation("Weight looks implausible.")
            profile.targetWeightKg != null && profile.targetWeightKg !in 20.0..500.0 -> AppError.Validation("Target weight looks implausible.")
            profile.trainingDaysPerWeek !in 0..14 -> AppError.Validation("Training days per week looks implausible.")
            else -> null
        }
    }

    private fun validateGoal(goal: NutritionGoal): AppError? {
        return when {
            goal.caloriesKcal !in 800.0..8000.0 -> AppError.Validation("Calorie goal looks implausible.")
            goal.proteinGrams < 0.0 -> AppError.Validation("Protein goal cannot be negative.")
            goal.carbohydrateGrams < 0.0 -> AppError.Validation("Carb goal cannot be negative.")
            goal.fatGrams < 0.0 -> AppError.Validation("Fat goal cannot be negative.")
            goal.fiberGrams < 0.0 -> AppError.Validation("Fiber goal cannot be negative.")
            goal.waterMilliliters !in 0.0..10000.0 -> AppError.Validation("Water goal looks implausible.")
            else -> null
        }
    }
}
