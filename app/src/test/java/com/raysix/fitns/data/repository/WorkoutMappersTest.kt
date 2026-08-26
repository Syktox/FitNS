package com.raysix.fitns.data.repository

import com.raysix.fitns.domain.model.ActiveWorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMappersTest {
    @Test
    fun activeWorkoutSet_isPersistedAsOneSetRegardlessOfDisplayNumber() {
        val entities = (1..3).map { setNumber ->
            ActiveWorkoutSet(
                setNumber = setNumber,
                weightKg = 100.0,
                repetitions = 8
            ).toEntity(
                workoutExerciseId = "workout-exercise-1",
                now = 1_000L
            )
        }

        assertEquals(listOf(1, 1, 1), entities.map { it.setCount })
    }

    @Test
    fun workoutEntity_persistsElapsedSessionMinutes() {
        val entity = newWorkoutEntity(
            id = "workout-1",
            now = 1_000L,
            endedAt = 1_000L + 3_750_000L
        )

        assertEquals(62, entity.durationMinutes)
    }
}
