package com.syktox.fitns.data.repository

import com.syktox.fitns.data.local.entity.BodyWeightEntryEntity
import com.syktox.fitns.data.local.entity.SyncStatus
import com.syktox.fitns.domain.model.BodyWeightLogEntry

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

