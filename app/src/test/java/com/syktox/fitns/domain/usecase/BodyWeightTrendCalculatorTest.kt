package com.syktox.fitns.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class BodyWeightTrendCalculatorTest {
    private val calculator = BodyWeightTrendCalculator()

    @Test
    fun movingAverage_usesPartialWindowAtBeginning() {
        val result = calculator.movingAverage(listOf(100.0, 99.0, 98.0, 97.0), windowSize = 3)

        assertEquals(listOf(100.0, 99.5, 99.0, 98.0), result)
    }
}

