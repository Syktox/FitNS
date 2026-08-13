package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutPlan
import com.raysix.fitns.domain.model.WorkoutPlanExercise
import com.raysix.fitns.domain.model.WorkoutSetInput
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildActiveWorkoutSessionUseCaseTest {
    private val exercise = Exercise(
        id = "bench",
        name = "Bench Press",
        muscleGroup = "Chest",
        machineType = "Barbell"
    )

    @Test
    fun fromPlan_prefillsEachSetFromTheMatchingSetInTheLatestSession() {
        val plan = WorkoutPlan(
            id = "push",
            name = "Push",
            focus = "Chest",
            estimatedMinutes = 45,
            exercises = listOf(
                WorkoutPlanExercise(exercise, targetSets = 3, targetRepMin = 8, targetRepMax = 12, restSeconds = 120)
            )
        )
        val history = listOf(
            WorkoutLogEntry(
                exercise = exercise,
                loggedAt = 1L,
                sets = listOf(
                    WorkoutSetInput(weightKg = 90.0, repetitions = 8, rpe = 8),
                    WorkoutSetInput(weightKg = 90.0, repetitions = 9, rpe = 8),
                    WorkoutSetInput(weightKg = 90.0, repetitions = 10, rpe = 8)
                )
            ),
            WorkoutLogEntry(
                exercise = exercise,
                loggedAt = 2L,
                sets = listOf(
                    WorkoutSetInput(weightKg = 100.0, repetitions = 8, rpe = 8),
                    WorkoutSetInput(weightKg = 100.0, repetitions = 9, rpe = 8),
                    WorkoutSetInput(weightKg = 100.0, repetitions = 10, rpe = 8)
                )
            )
        )

        val session = BuildActiveWorkoutSessionUseCase().fromPlan(plan, history)
        val sets = session.exercises.single().sets

        assertEquals(listOf(8, 9, 10), sets.map { it.repetitions })
        assertEquals(listOf(100.0, 100.0, 100.0), sets.map { it.weightKg })
        assertEquals(2L, sets.last().previousPerformance?.loggedAt)
    }
}
