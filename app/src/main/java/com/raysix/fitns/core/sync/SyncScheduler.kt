package com.raysix.fitns.core.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastEnqueueAt = 0L
    private var trailingJob: Job? = null
    private var pausedForLocalDataDeletion = false

    @Synchronized
    fun schedule() {
        if (pausedForLocalDataDeletion) return
        val now = System.currentTimeMillis()
        val elapsed = now - lastEnqueueAt
        if (elapsed >= CoalescingWindowMillis) {
            trailingJob?.cancel()
            trailingJob = null
            lastEnqueueAt = now
            enqueueWork()
            return
        }
        trailingJob?.cancel()
        trailingJob = scope.launch {
            delay(CoalescingWindowMillis - elapsed)
            synchronized(this@SyncScheduler) {
                trailingJob = null
                if (!pausedForLocalDataDeletion) {
                    lastEnqueueAt = System.currentTimeMillis()
                    enqueueWork()
                }
            }
        }
    }

    /**
     * Prevents new sync work and waits until WorkManager has applied the
     * cancellation to the complete unique sync chain.
     */
    suspend fun pauseAndCancelPendingWork() {
        synchronized(this) {
            pausedForLocalDataDeletion = true
            trailingJob?.cancel()
            trailingJob = null
        }
        try {
            withContext(Dispatchers.IO) {
                WorkManager.getInstance(context)
                    .cancelUniqueWork(SyncWorkName)
                    .result
                    .get()
            }
        } catch (exception: Exception) {
            resumeScheduling()
            throw exception
        }
    }

    @Synchronized
    fun resumeScheduling() {
        pausedForLocalDataDeletion = false
        lastEnqueueAt = 0L
    }

    private fun enqueueWork() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(30))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorkName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request
        )
    }

    companion object {
        const val SyncWorkName = "fitns-sync"
        const val CoalescingWindowMillis = 1_000L
    }
}
