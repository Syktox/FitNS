package com.raysix.fitns.core.sync

import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.data.local.entity.SyncQueueItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import java.util.UUID
import javax.inject.Inject

class SyncQueueWriter @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncScheduler: SyncScheduler
) {
    suspend fun enqueue(entityType: String, entityId: String, operation: String, payloadJson: String) {
        val now = System.currentTimeMillis()
        syncQueueDao.upsert(
            SyncQueueItemEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                operation = operation,
                payloadJson = payloadJson,
                idempotencyKey = "$entityType-$entityId-$operation-$now",
                retryCount = 0,
                nextAttemptAt = null,
                lastError = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                syncStatus = SyncStatus.PendingSync,
                serverVersion = null
            )
        )
        syncScheduler.schedule()
    }
}

