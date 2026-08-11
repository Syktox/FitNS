package com.raysix.fitns.domain.usecase

import com.raysix.fitns.domain.model.ActiveWorkoutExercise
import com.raysix.fitns.domain.model.ActiveWorkoutSession
import com.raysix.fitns.domain.model.ActiveWorkoutSet
import com.raysix.fitns.domain.model.Exercise
import com.raysix.fitns.domain.model.PersonalRecordType
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.WorkoutSetInput
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalRecordDetectorTest {
    private val detector = PersonalRecordDetector(EstimatedOneRepMaxCalculator())
    private val bench = Exercise(
        id = "bench",
        name = "Bench Press",
        muscleGroup = "Chest",
        machineType = "Barbell"
    )

    @Test
    fun detect_reportsWeightRepsOneRepMaxAndSessionVolumeRecords() {
        val history = listOf(
            WorkoutLogEntry(
                id = "old-workout",
                exercise = bench,
                sets = listOf(WorkoutSetInput(weightKg = 100.0, repetitions = 8, rpe = 8)),
                loggedAt = 1L
            )
        )
        val session = ActiveWorkoutSession(
            sourcePlanId = "plan",
            name = "Push",
            exercises = listOf(
                ActiveWorkoutExercise(
                    exercise = bench,
                    sortOrder = 0,
                    targetRepMin = 8,
                    targetRepMax = 12,
                    restSeconds = 90,
                    sets = listOf(
                        ActiveWorkoutSet(
                            setNumber = 1,
                            weightKg = 102.5,
                            repetitions = 9,
                            completedAt = 2L
                        )
                    )
                )
            )
        )

        val types = detector.detect(session, history).map { it.type }.toSet()

        assertTrue(PersonalRecordType.HighestWeight in types)
        assertTrue(PersonalRecordType.HighestRepsAtWeight in types)
        assertTrue(PersonalRecordType.HighestEstimatedOneRepMax in types)
        assertTrue(PersonalRecordType.HighestSessionVolume in types)
    }
}
