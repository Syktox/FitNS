package com.raysix.fitns.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raysix.fitns.core.network.N8nApiService
import com.raysix.fitns.core.network.N8nServiceFactory
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.data.local.entity.SyncQueueItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val settingsRepository: SettingsRepository,
    private val serviceFactory: N8nServiceFactory
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val settings = settingsRepository.observeN8nSettings().first()
        if (!settings.syncEnabled) return Result.success()

        val baseUrl = serviceFactory.normalizeBaseUrl(settings.baseUrl) ?: return Result.failure()

        val service = serviceFactory.serviceFor(baseUrl)
        val authorization = settingsRepository.readBearerToken().asAuthorizationHeader()
        repeat(MaxBatchesPerRun) {
            val dueItems = syncQueueDao.pendingDueItems(System.currentTimeMillis())
            if (dueItems.isEmpty()) return Result.success()
            var shouldRetry = false

            dueItems.forEach { item ->
                val result = runCatching { sendItem(service, item, authorization) }
                if (result.isSuccess) {
                    syncQueueDao.updateStatus(item.id, SyncStatus.Synced, System.currentTimeMillis())
                } else {
                    val error = result.exceptionOrNull()
                    if (error is PermanentSyncException) {
                        syncQueueDao.markTerminalFailure(
                            id = item.id,
                            updatedAt = System.currentTimeMillis(),
                            lastError = error.safeMessage()
                        )
                    } else {
                        shouldRetry = true
                        syncQueueDao.markRetry(
                            id = item.id,
                            status = SyncStatus.Failed,
                            updatedAt = System.currentTimeMillis(),
                            nextAttemptAt = null,
                            lastError = error?.safeMessage().orEmpty()
                        )
                    }
                }
            }
            if (shouldRetry) return Result.retry()
        }
        return Result.retry()
    }

    private suspend fun sendItem(service: N8nApiService, item: SyncQueueItemEntity, authorization: String?) {
        val body = item.payloadJson.toRequestBody(JsonMediaType)
        val response = when (item.entityType) {
            EntityTypeFoodEntry -> service.syncNutrition(authorization, item.idempotencyKey, body)
            EntityTypeWorkout -> service.syncWorkout(authorization, item.idempotencyKey, body)
            EntityTypeBodyWeight -> service.syncBodyWeight(authorization, item.idempotencyKey, body)
            EntityTypeUserProfile -> service.syncProfile(authorization, item.idempotencyKey, body)
            else -> error("Unsupported sync entity type: ${item.entityType}")
        }
        if (!response.isSuccessful) {
            val code = response.code()
            if (code in 400..499 && code !in setOf(408, 429)) {
                throw PermanentSyncException("HTTP $code")
            }
            throw IOException("HTTP $code")
        }
    }

    private fun String?.asAuthorizationHeader(): String? {
        val token = this?.trim().orEmpty()
        return if (token.isBlank()) null else "Bearer $token"
    }

    private fun Throwable.safeMessage(): String {
        return message?.take(180) ?: this::class.simpleName.orEmpty()
    }

    private companion object {
        val JsonMediaType = "application/json; charset=utf-8".toMediaType()
        const val EntityTypeFoodEntry = "FoodEntry"
        const val EntityTypeWorkout = "Workout"
        const val EntityTypeBodyWeight = "BodyWeight"
        const val EntityTypeUserProfile = "UserProfile"
        const val MaxBatchesPerRun = 8
    }
}

private class PermanentSyncException(message: String) : IOException(message)
