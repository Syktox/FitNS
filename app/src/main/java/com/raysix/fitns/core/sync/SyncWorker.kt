package com.raysix.fitns.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.raysix.fitns.core.network.N8nApiService
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.data.local.entity.SyncQueueItemEntity
import com.raysix.fitns.data.local.entity.SyncStatus
import com.raysix.fitns.domain.repository.SettingsRepository
import com.squareup.moshi.Moshi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import kotlin.math.pow

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val settings = settingsRepository.observeN8nSettings().first()
        if (!settings.syncEnabled) return Result.success()

        val baseUrl = normalizeBaseUrl(settings.baseUrl) ?: return Result.failure()
        val dueItems = syncQueueDao.pendingDueItems(System.currentTimeMillis())
        if (dueItems.isEmpty()) return Result.success()

        val service = serviceFor(baseUrl)
        val authorization = settingsRepository.readBearerToken().asAuthorizationHeader()
        var shouldRetry = false

        dueItems.forEach { item ->
            val result = runCatching { sendItem(service, item, authorization) }
            if (result.isSuccess) {
                syncQueueDao.updateStatus(item.id, SyncStatus.Synced, System.currentTimeMillis())
            } else {
                shouldRetry = true
                syncQueueDao.markRetry(
                    id = item.id,
                    status = SyncStatus.Failed,
                    updatedAt = System.currentTimeMillis(),
                    nextAttemptAt = nextAttemptAt(item.retryCount),
                    lastError = result.exceptionOrNull()?.safeMessage().orEmpty()
                )
            }
        }

        return if (shouldRetry) Result.retry() else Result.success()
    }

    private suspend fun sendItem(service: N8nApiService, item: SyncQueueItemEntity, authorization: String?) {
        val body = item.payloadJson.toRequestBody(JsonMediaType)
        val response = when (item.entityType) {
            EntityTypeFoodEntry -> service.syncNutrition(authorization, item.idempotencyKey, body)
            EntityTypeWorkout -> service.syncWorkout(authorization, item.idempotencyKey, body)
            EntityTypeBodyWeight -> service.syncBodyWeight(authorization, item.idempotencyKey, body)
            else -> error("Unsupported sync entity type: ${item.entityType}")
        }
        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code()}")
        }
    }

    private fun String?.asAuthorizationHeader(): String? {
        val token = this?.trim().orEmpty()
        return if (token.isBlank()) null else "Bearer $token"
    }

    private fun serviceFor(baseUrl: String): N8nApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(N8nApiService::class.java)
    }

    private fun normalizeBaseUrl(baseUrl: String): String? {
        val trimmed = baseUrl.trim()
        if (!trimmed.startsWith("https://")) return null
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun nextAttemptAt(retryCount: Int): Long {
        val delayMinutes = 2.0.pow(retryCount.coerceAtMost(6)).toLong()
        return System.currentTimeMillis() + delayMinutes * 60_000
    }

    private fun Throwable.safeMessage(): String {
        return message?.take(180) ?: this::class.simpleName.orEmpty()
    }

    private companion object {
        val JsonMediaType = "application/json; charset=utf-8".toMediaType()
        const val EntityTypeFoodEntry = "FoodEntry"
        const val EntityTypeWorkout = "Workout"
        const val EntityTypeBodyWeight = "BodyWeight"
    }
}

