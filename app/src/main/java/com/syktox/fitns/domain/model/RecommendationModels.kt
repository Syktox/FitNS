package com.syktox.fitns.domain.model

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

