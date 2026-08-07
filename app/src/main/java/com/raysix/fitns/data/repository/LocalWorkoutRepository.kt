package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncPayloadFactory
import com.raysix.fitns.core.sync.SyncQueueWriter
import com.raysix.fitns.data.local.dao.WorkoutDao
import com.raysix.fitns.data.local.entity.ExerciseEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.data.local.entity.WorkoutExerciseEntity
import com.raysix.fitns.data.local.entity.WorkoutSetEntity
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import com.raysix.fitns.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID
import javax.inject.Inject

class LocalWorkoutRepository @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val syncQueueWriter: SyncQueueWriter,
    private val syncPayloadFactory: SyncPayloadFactory
) : WorkoutRepository {
    override fun observeExercises(): Flow<List<Exercise>> {
        return combine(
            workoutDao.observeExercises(),
            workoutDao.observeWorkoutExercises(),
            workoutDao.observeWorkoutSets()
        ) { exercises, workoutExercises, sets ->
            val seededExercises = mergeDefaultExercises(exercises)
            seededExercises.map { exercise ->
                val relatedWorkoutExerciseIds = workoutExercises
                    .filter { it.exerciseId == exercise.id }
                    .map { it.id }
                    .toSet()
                val latestSet = sets.firstOrNull { it.workoutExerciseId in relatedWorkoutExerciseIds }
                val best = sets
                    .filter { it.workoutExerciseId in relatedWorkoutExerciseIds }
                    .maxOfOrNull { it.weightKg }
                exercise.toDomain(
                    lastWeightKg = latestSet?.weightKg,
                    lastRepetitions = latestSet?.repetitions,
                    lastSets = latestSet?.setCount,
                    personalBestKg = best ?: latestSet?.weightKg
                )
            }
        }
    }

    override fun observeHistory(): Flow<List<WorkoutLogEntry>> {
        return combine(
            observeExercises(),
            workoutDao.observeWorkoutExercises(),
            workoutDao.observeWorkoutSets()
        ) { exercises, workoutExercises, sets ->
            sets.mapNotNull { set ->
                val workoutExercise = workoutExercises.firstOrNull { it.id == set.workoutExerciseId }
                val exercise = exercises.firstOrNull { it.id == workoutExercise?.exerciseId }
                if (exercise == null) {
                    null
                } else {
                    WorkoutLogEntry(
                        id = workoutExercise?.workoutId ?: set.id,
                        exercise = exercise,
                        sets = listOf(set.toDomain()),
                        notes = workoutExercise?.notes.orEmpty(),
                        loggedAt = set.createdAt
                    )
                }
            }
        }
    }

    override fun observeWorkoutPlans(): Flow<List<WorkoutPlan>> {
        return combine(
            workoutDao.observeWorkoutPlans(),
            workoutDao.observeWorkoutPlanExercises(),
            observeExercises()
        ) { plans, planExercises, exercises ->
            plans.map { plan ->
                val items = planExercises
                    .filter { it.planId == plan.id }
                    .sortedBy { it.sortOrder }
                    .mapNotNull { planExercise ->
                        val exercise = exercises.firstOrNull { it.id == planExercise.exerciseId }
                            ?: return@mapNotNull null
                        WorkoutPlanExercise(
                            exercise = exercise,
                            targetSets = planExercise.targetSets,
                            targetRepMin = planExercise.targetRepMin,
                            targetRepMax = planExercise.targetRepMax,
                            restSeconds = planExercise.restSeconds,
                            notes = planExercise.notes
                        )
                    }
                WorkoutPlan(
                    id = plan.id,
                    name = plan.name,
                    focus = plan.focus,
                    estimatedMinutes = plan.estimatedMinutes,
                    exercises = items
                )
            }
        }
    }

    override suspend fun addExercise(exercise: Exercise): AppResult<Unit> {
        val error = validate(exercise)
        if (error != null) return AppResult.Failure(error)
        workoutDao.upsertExercise(exercise.toEntity())
        return AppResult.Success(Unit)
    }

    override suspend fun addWorkout(entry: WorkoutLogEntry): AppResult<Unit> {
        val set = entry.sets.firstOrNull()
            ?: return AppResult.Failure(AppError.Validation("At least one set is required."))
        val error = validate(set)
        if (error != null) return AppResult.Failure(error)

        val now = System.currentTimeMillis()
        val workoutId = UUID.randomUUID().toString()
        val workoutExerciseId = UUID.randomUUID().toString()
        val syncEntry = entry.copy(id = workoutId, loggedAt = now)
        workoutDao.addWorkoutSet(
            exercise = syncEntry.exercise.toEntity(now),
            workout = newWorkoutEntity(workoutId, now),
            workoutExercise = newWorkoutExerciseEntity(
                id = workoutExerciseId,
                workoutId = workoutId,
                exerciseId = syncEntry.exercise.id,
                notes = syncEntry.notes,
                now = now
            ),
            set = set.toEntity(workoutExerciseId, now)
        )
        syncQueueWriter.enqueue(
            entityType = EntityTypeWorkout,
            entityId = workoutId,
            operation = OperationUpsert,
            payloadJson = syncPayloadFactory.workout(syncEntry, OperationUpsert)
        )
        return AppResult.Success(Unit)
    }

    override suspend fun saveWorkoutPlan(plan: WorkoutPlan): AppResult<Unit> {
        val error = validate(plan)
        if (error != null) return AppResult.Failure(error)

        val now = System.currentTimeMillis()
        workoutDao.upsertWorkoutPlanWithExercises(
            plan = plan.toEntity(now),
            exercises = plan.exercises.mapIndexed { index, exercise ->
                exercise.toEntity(plan.id, index, now)
            }
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteWorkoutPlan(plan: WorkoutPlan): AppResult<Unit> {
        workoutDao.softDeleteWorkoutPlanCascade(
            planId = plan.id,
            deletedAt = System.currentTimeMillis()
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteWorkout(entry: WorkoutLogEntry): AppResult<Unit> {
        val workoutExerciseIds = workoutDao.workoutExercisesForWorkout(entry.id).map { it.id }
        if (workoutExerciseIds.isEmpty()) return AppResult.Failure(AppError.NotFound)
        workoutDao.softDeleteWorkoutCascade(
            workoutId = entry.id,
            workoutExerciseIds = workoutExerciseIds,
            deletedAt = System.currentTimeMillis()
        )
        syncQueueWriter.enqueue(
            entityType = EntityTypeWorkout,
            entityId = entry.id,
            operation = OperationDelete,
            payloadJson = syncPayloadFactory.workout(entry, OperationDelete)
        )
        return AppResult.Success(Unit)
    }

    private fun validate(set: WorkoutSetInput): AppError? {
        return when {
            set.weightKg < 0.0 -> AppError.Validation("Workout weight cannot be negative.")
            set.repetitions < 0 -> AppError.Validation("Reps cannot be negative.")
            set.sets < 1 -> AppError.Validation("Set count must be at least one.")
            set.rpe != null && set.rpe !in 1..10 -> AppError.Validation("RPE must be between 1 and 10.")
            else -> null
        }
    }

    private fun validate(exercise: Exercise): AppError? {
        return when {
            exercise.name.isBlank() -> AppError.Validation("Exercise name is required.")
            exercise.muscleGroup.isBlank() -> AppError.Validation("Muscle group is required.")
            exercise.machineType.isBlank() -> AppError.Validation("Equipment type is required.")
            else -> null
        }
    }

    private fun validate(plan: WorkoutPlan): AppError? {
        return when {
            plan.name.isBlank() -> AppError.Validation("Plan name is required.")
            plan.exercises.isEmpty() -> AppError.Validation("Add at least one exercise to the plan.")
            plan.exercises.any { it.targetSets < 1 } -> AppError.Validation("Target sets must be at least one.")
            plan.exercises.any { it.targetRepMin < 1 || it.targetRepMax < it.targetRepMin } -> AppError.Validation("Use a valid rep range.")
            plan.exercises.any { it.restSeconds < 0 } -> AppError.Validation("Rest time cannot be negative.")
            else -> null
        }
    }

    private fun WorkoutSetEntity.toDomain(): WorkoutSetInput {
        return WorkoutSetInput(
            weightKg = weightKg,
            repetitions = repetitions,
            rpe = rpe,
            sets = setCount,
            isPerSide = isPerSide
        )
    }

    private fun defaultExercises(): List<ExerciseEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ExerciseEntity(
                id = "exercise-leg-press",
                name = "Leg Press",
                muscleGroup = "Legs",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Seat position 4",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-chest-press",
                name = "Chest Press",
                muscleGroup = "Chest",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-lat-pulldown",
                name = "Lat Pulldown",
                muscleGroup = "Back",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Use a controlled pull and full stretch.",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-seated-row",
                name = "Seated Row",
                muscleGroup = "Back",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Keep torso stable through the pull.",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-shoulder-press",
                name = "Shoulder Press",
                muscleGroup = "Shoulders",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Stop just short of elbow lockout.",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-leg-curl",
                name = "Leg Curl",
                muscleGroup = "Legs",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Pause briefly in the contracted position.",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            ),
            ExerciseEntity(
                id = "exercise-leg-extension",
                name = "Leg Extension",
                muscleGroup = "Legs",
                machineType = "Machine",
                gym = "Gym",
                machineCode = null,
                notes = "Control the lowering phase.",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.LocalOnly,
                serverVersion = null
            )
        )
    }

    private fun mergeDefaultExercises(exercises: List<ExerciseEntity>): List<ExerciseEntity> {
        val existingIds = exercises.map { it.id }.toSet()
        return (exercises + defaultExercises().filterNot { it.id in existingIds })
            .sortedBy { it.name }
    }

    private companion object {
        const val EntityTypeWorkout = "Workout"
        const val OperationUpsert = "upsert"
        const val OperationDelete = "delete"
    }
}
