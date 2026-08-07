package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncPayloadFactory
import com.raysix.fitns.core.sync.SyncQueueWriter
import com.raysix.fitns.data.local.dao.BodyWeightDao
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.repository.BodyWeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalBodyWeightRepository @Inject constructor(
    private val bodyWeightDao: BodyWeightDao,
    private val syncQueueWriter: SyncQueueWriter,
    private val syncPayloadFactory: SyncPayloadFactory
) : BodyWeightRepository {
    override fun observeHistory(): Flow<List<BodyWeightLogEntry>> {
        return bodyWeightDao.observeEntries().map { entries ->
            entries.map { it.toDomain() }
        }
    }

    override suspend fun addEntry(entry: BodyWeightLogEntry): AppResult<Unit> {
        val error = validate(entry)
        if (error != null) return AppResult.Failure(error)
        bodyWeightDao.upsert(entry.toEntity())
        syncQueueWriter.enqueue(
            entityType = EntityTypeBodyWeight,
            entityId = entry.id,
            operation = OperationUpsert,
            payloadJson = syncPayloadFactory.bodyWeight(entry, OperationUpsert)
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteEntry(entry: BodyWeightLogEntry): AppResult<Unit> {
        val existing = bodyWeightDao.findEntry(entry.id)?.toDomain()
            ?: return AppResult.Failure(AppError.NotFound)
        bodyWeightDao.softDelete(entry.id, System.currentTimeMillis())
        syncQueueWriter.enqueue(
            entityType = EntityTypeBodyWeight,
            entityId = entry.id,
            operation = OperationDelete,
            payloadJson = syncPayloadFactory.bodyWeight(existing, OperationDelete)
        )
        return AppResult.Success(Unit)
    }

    private fun validate(entry: BodyWeightLogEntry): AppError? {
        return when {
            entry.weightKg <= 0.0 -> AppError.Validation("Weight must be greater than zero.")
            entry.weightKg > 500.0 -> AppError.Validation("Weight looks implausibly high.")
            else -> null
        }
    }

    private companion object {
        const val EntityTypeBodyWeight = "BodyWeight"
        const val OperationUpsert = "upsert"
        const val OperationDelete = "delete"
    }
}
