package com.raysix.fitns.domain.model

import java.util.UUID

data class BodyWeightLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val measuredAt: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val notes: String = ""
)

