package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncPayloadFactory
import com.raysix.fitns.core.sync.SyncQueueWriter
import com.raysix.fitns.core.undo.AppUndoRedoManager
import com.raysix.fitns.core.undo.UndoRedoAction
import com.raysix.fitns.data.local.dao.ProfileDao
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.model.DefaultNutrientTargets
import com.raysix.fitns.domain.model.DefaultUserProfileId
import com.raysix.fitns.domain.model.NutrientTarget
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.model.VersionedNutritionGoal
import com.raysix.fitns.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import com.raysix.fitns.data.local.FitNsDatabase
import androidx.room.withTransaction

class LocalProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val database: FitNsDatabase,
    private val syncQueueWriter: SyncQueueWriter,
    private val syncPayloadFactory: SyncPayloadFactory,
    private val undoRedoManager: AppUndoRedoManager
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

    override fun observeNutritionGoalForDate(dateMillis: Long): Flow<NutritionGoal> {
        return profileDao.observeNutritionGoalForDate(DefaultUserProfileId, dateMillis).map { entity ->
            entity?.toDomain() ?: DefaultNutritionGoal
        }
    }

    override fun observeNutritionGoalHistory(): Flow<List<VersionedNutritionGoal>> {
        return profileDao.observeNutritionGoalHistory(DefaultUserProfileId).map { entities ->
            entities.map { entity ->
                VersionedNutritionGoal(
                    goal = entity.toDomain(),
                    validFrom = entity.validFrom
                )
            }
        }
    }

    override fun observeNutrientTargets(): Flow<List<NutrientTarget>> {
        return profileDao.observeActiveNutrientTargets(DefaultUserProfileId).map { entities ->
            entities.map { it.toDomain() }.ifEmpty { DefaultNutrientTargets.targets }
        }
    }

    override suspend fun saveProfile(profile: UserProfile): AppResult<Unit> {
        val error = validateProfile(profile)
        if (error != null) return AppResult.Failure(error)
        val previous = observeProfile().first()
        val savedProfile = profile.copy(id = DefaultUserProfileId)
        database.withTransaction {
            profileDao.upsertProfile(savedProfile.toEntity())
            syncQueueWriter.enqueueOnly(
                entityType = EntityTypeUserProfile,
                entityId = savedProfile.id,
                operation = OperationUpsert,
                payloadJson = syncPayloadFactory.profile(savedProfile, OperationUpsert)
            )
        }
        syncQueueWriter.schedule()
        recordUndo(
            label = "profile",
            undo = { saveProfile(previous) },
            redo = { saveProfile(savedProfile) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveNutritionGoal(goal: NutritionGoal): AppResult<Unit> {
        val error = validateGoal(goal)
        if (error != null) return AppResult.Failure(error)
        val previous = observeNutritionGoal().first()
        val now = System.currentTimeMillis()
        profileDao.replaceOpenNutritionGoal(goal.toVersionedEntity(now), now)
        recordUndo(
            label = "nutrition goals",
            undo = { saveNutritionGoal(previous) },
            redo = { saveNutritionGoal(goal) }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveNutrientTargets(targets: List<NutrientTarget>): AppResult<Unit> {
        val previous = observeNutrientTargets().first()
        val invalidTarget = targets.firstOrNull {
            !it.targetAmount.isFinite() || it.targetAmount < 0.0 || it.unit.isBlank()
        }
        if (invalidTarget != null) {
            return AppResult.Failure(
                AppError.Validation("${invalidTarget.key.label} needs a finite, non-negative target and a unit.")
            )
        }
        if (targets.distinctBy { it.key }.size != targets.size) {
            return AppResult.Failure(AppError.Validation("Each nutrient can only have one active target."))
        }
        val now = System.currentTimeMillis()
        profileDao.replaceOpenNutrientTargets(
            userProfileId = DefaultUserProfileId,
            targets = targets.map { it.toEntity(now) },
            validTo = now
        )
        recordUndo(
            label = "nutrient targets",
            undo = { saveNutrientTargets(previous) },
            redo = { saveNutrientTargets(targets) }
        )
        return AppResult.Success(Unit)
    }

    private fun recordUndo(label: String, undo: suspend () -> Unit, redo: suspend () -> Unit) {
        undoRedoManager.record(UndoRedoAction(label = label, undo = undo, redo = redo))
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
            !goal.proteinGrams.isFinite() || goal.proteinGrams < 0.0 -> AppError.Validation("Protein goal must be a finite, non-negative number.")
            !goal.carbohydrateGrams.isFinite() || goal.carbohydrateGrams < 0.0 -> AppError.Validation("Carb goal must be a finite, non-negative number.")
            !goal.fatGrams.isFinite() || goal.fatGrams < 0.0 -> AppError.Validation("Fat goal must be a finite, non-negative number.")
            !goal.fiberGrams.isFinite() || goal.fiberGrams < 0.0 -> AppError.Validation("Fiber goal must be a finite, non-negative number.")
            goal.waterMilliliters !in 0.0..10000.0 -> AppError.Validation("Water goal looks implausible.")
            else -> null
        }
    }

    private companion object {
        const val EntityTypeUserProfile = "UserProfile"
        const val OperationUpsert = "upsert"
    }
}
