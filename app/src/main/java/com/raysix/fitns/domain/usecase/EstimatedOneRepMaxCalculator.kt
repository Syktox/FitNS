package com.raysix.fitns.domain.usecase

import kotlin.math.roundToInt
import javax.inject.Inject

class EstimatedOneRepMaxCalculator @Inject constructor() {
    fun calculate(weightKg: Double, repetitions: Int): Double {
        if (weightKg <= 0.0 || repetitions <= 0) return 0.0
        return weightKg * (1.0 + repetitions / 30.0)
    }

    fun calculateRounded(weightKg: Double, repetitions: Int): Int {
        return calculate(weightKg, repetitions).roundToInt()
    }
}
