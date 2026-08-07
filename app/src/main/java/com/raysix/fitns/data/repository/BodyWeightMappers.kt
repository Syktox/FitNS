package com.raysix.fitns.data.repository

import com.raysix.fitns.data.local.entity.BodyWeightEntryEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.model.BodyWeightLogEntry

fun BodyWeightEntryEntity.toDomain(): BodyWeightLogEntry {
    return BodyWeightLogEntry(
        id = id,
        measuredAt = measuredAt,
        weightKg = weightKg,
        notes = notes
    )
}

fun BodyWeightLogEntry.toEntity(now: Long = System.currentTimeMillis()): BodyWeightEntryEntity {
    return BodyWeightEntryEntity(
        id = id,
        measuredAt = measuredAt,
        weightKg = weightKg,
        notes = notes,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        syncStatus = SyncStatus.PendingSync,
        serverVersion = null
    )
}

