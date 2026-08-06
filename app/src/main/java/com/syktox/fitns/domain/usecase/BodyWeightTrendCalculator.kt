package com.syktox.fitns.domain.usecase

class BodyWeightTrendCalculator {
    fun movingAverage(values: List<Double>, windowSize: Int): List<Double> {
        require(windowSize > 0) { "Window size must be greater than zero." }
        return values.indices.map { index ->
            val from = (index - windowSize + 1).coerceAtLeast(0)
            values.subList(from, index + 1).average()
        }
    }
}

