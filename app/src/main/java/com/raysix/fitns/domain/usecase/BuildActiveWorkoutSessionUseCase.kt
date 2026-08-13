package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.PreviousPerformance
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import javax.inject.Inject

class BuildActiveWorkoutSessionUseCase @Inject constructor() {
    fun fromPlan(plan: WorkoutPlan, history: List<WorkoutLogEntry>): ActiveWorkoutSession {
        return ActiveWorkoutSession(
            sourcePlanId = plan.id,
            name = plan.name,
            exercises = plan.exercises.mapIndexed { index, planExercise ->
                val previousEntry = history
                    .filter { it.exercise.id == planExercise.exercise.id }
                    .maxByOrNull { it.loggedAt }
                val previous = previousEntry
                    ?.sets
                    ?.firstOrNull()
                    ?.let { set ->
                        PreviousPerformance(
                            exerciseId = planExercise.exercise.id,
                            weightKg = set.weightKg,
                            repetitions = set.repetitions,
                            loggedAt = previousEntry.loggedAt
                        )
                    }
                ActiveWorkoutExercise(
                    exercise = planExercise.exercise,
                    sortOrder = index,
                    targetRepMin = planExercise.targetRepMin,
                    targetRepMax = planExercise.targetRepMax,
                    restSeconds = planExercise.restSeconds,
                    sets = (1..planExercise.targetSets).map { setNumber ->
                        ActiveWorkoutSet(
                            setNumber = setNumber,
                            weightKg = previous?.weightKg ?: 0.0,
                            repetitions = previous?.repetitions ?: planExercise.targetRepMin,
                            previousPerformance = previous,
                            restSeconds = planExercise.restSeconds
                        )
                    }
                )
            }
        )
    }
}
