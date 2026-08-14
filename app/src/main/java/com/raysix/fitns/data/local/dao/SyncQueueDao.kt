package com.raysix.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raysix.fitns.data.local.entity.SyncQueueItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query(
        """
        SELECT * FROM sync_queue_items
        WHERE deletedAt IS NULL
        AND syncStatus IN ('PendingSync', 'Failed')
        AND (nextAttemptAt IS NULL OR nextAttemptAt <= :now)
        ORDER BY createdAt ASC
        LIMIT :limit
        """
    )
    suspend fun pendingDueItems(
        now: Long,
        limit: Int = 25
    ): List<SyncQueueItemEntity>

    @Query("SELECT COUNT(*) FROM sync_queue_items WHERE deletedAt IS NULL AND syncStatus IN ('PendingSync', 'Failed')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue_items WHERE deletedAt IS NULL AND syncStatus = 'Conflict'")
    fun observeConflictCount(): Flow<Int>

    @Query("SELECT lastError FROM sync_queue_items WHERE deletedAt IS NULL AND syncStatus = 'Conflict' ORDER BY updatedAt DESC LIMIT 1")
    fun observeLatestConflictError(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncQueueItemEntity)

    @Query(
        """
        UPDATE sync_queue_items
        SET syncStatus = :status,
            updatedAt = :updatedAt,
            lastError = NULL,
            nextAttemptAt = NULL
        WHERE id = :id
        """
    )
    suspend fun updateStatus(id: String, status: SyncStatus, updatedAt: Long)

    @Query(
        """
        UPDATE sync_queue_items
        SET syncStatus = :status,
            retryCount = retryCount + 1,
            updatedAt = :updatedAt,
            nextAttemptAt = :nextAttemptAt,
            lastError = :lastError
        WHERE id = :id
        """
    )
    suspend fun markRetry(id: String, status: SyncStatus, updatedAt: Long, nextAttemptAt: Long?, lastError: String)

    @Query(
        """
        UPDATE sync_queue_items
        SET syncStatus = 'Conflict',
            updatedAt = :updatedAt,
            nextAttemptAt = NULL,
            lastError = :lastError
        WHERE id = :id
        """
    )
    suspend fun markTerminalFailure(id: String, updatedAt: Long, lastError: String)

    @Query(
        """
        UPDATE sync_queue_items
        SET syncStatus = 'PendingSync',
            retryCount = 0,
            updatedAt = :updatedAt,
            nextAttemptAt = NULL,
            lastError = NULL
        WHERE deletedAt IS NULL AND syncStatus = 'Conflict'
        """
    )
    suspend fun requeueConfigurationFailures(updatedAt: Long)
}
