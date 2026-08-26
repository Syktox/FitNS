package com.raysix.fitns.feature.workout

import com.raysix.fitns.domain.model.ActiveWorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSessionStateTest {
    @Test
    fun completionValidation_acceptsBodyweightSetWithValidRepsAndEffort() {
        val set = ActiveWorkoutSet(
            setNumber = 1,
            weightKg = 0.0,
            repetitions = 12,
            rpe = 8,
            rir = 2
        )

        assertNull(set.completionValidationMessage())
    }

    @Test
    fun completionValidation_rejectsMissingRepetitions() {
        val set = ActiveWorkoutSet(
            setNumber = 1,
            weightKg = 40.0,
            repetitions = 0
        )

        assertEquals(
            "Enter at least one repetition before completing the set.",
            set.completionValidationMessage()
        )
    }

    @Test
    fun completionValidation_rejectsNonFiniteWeight() {
        val set = ActiveWorkoutSet(
            setNumber = 1,
            weightKg = Double.NaN,
            repetitions = 8
        )

        assertEquals(
            "Enter a valid, non-negative weight before completing the set.",
            set.completionValidationMessage()
        )
    }

    @Test
    fun remainingRestTimerSeconds_roundsUpAndStopsAtZero() {
        val now = 10_000L
        val deadline = restTimerDeadline(nowMillis = now, seconds = 90)

        assertEquals(90, remainingRestTimerSeconds(deadline, now))
        assertEquals(1, remainingRestTimerSeconds(deadline, deadline - 1L))
        assertEquals(0, remainingRestTimerSeconds(deadline, deadline + 1L))
    }

    @Test
    fun decimalInput_acceptsAndNormalizesGermanSeparator() {
        assertEquals("82.5", "82,5".normalizedUnsignedDecimalInputOrNull())
        assertEquals("82.5", "82.5".normalizedUnsignedDecimalInputOrNull())
        assertNull("82,5.1".normalizedUnsignedDecimalInputOrNull())
        assertNull("-82,5".normalizedUnsignedDecimalInputOrNull())
    }
}
