package com.raysix.fitns.domain.model

enum class RecommendationSeverity {
    Info,
    Attention,
    Positive
}

data class RecommendationItem(
    val category: String,
    val message: String,
    val rationale: String,
    val severity: RecommendationSeverity,
    val confidence: Double
)

data class CoachMetric(
    val label: String,
    val value: Double,
    val target: Double,
    val unit: String,
    val status: String
)

data class TrainingLoadResult(
    val weeklySetCount: Int,
    val todaySetCount: Int,
    val weeklyVolumeKg: Double,
    val daysSinceLastWorkout: Int?
)

data class ReadinessResult(
    val title: String,
    val summary: String,
    val status: String,
    val weeklySetCount: Int,
    val daysSinceLastWorkout: Int?
)

data class NutritionAdherenceResult(
    val score: Double,
    val focus: String,
    val metrics: List<CoachMetric>
)

data class DailyCoachResult(
    val score: Int,
    val title: String,
    val summary: String,
    val focus: String,
    val metrics: List<CoachMetric>,
    val readiness: ReadinessResult
)
