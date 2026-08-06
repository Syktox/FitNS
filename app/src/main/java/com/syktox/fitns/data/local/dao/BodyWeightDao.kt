package com.syktox.fitns.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.syktox.fitns.data.local.entity.BodyWeightEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {
    @Query("SELECT * FROM body_weight_entries WHERE deletedAt IS NULL ORDER BY measuredAt DESC")
    fun observeEntries(): Flow<List<BodyWeightEntryEntity>>

    @Query("SELECT * FROM body_weight_entries WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun findEntry(id: String): BodyWeightEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BodyWeightEntryEntity)

    @Query("UPDATE body_weight_entries SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PendingSync' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)
}
