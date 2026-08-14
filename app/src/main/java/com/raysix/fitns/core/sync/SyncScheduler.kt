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

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastEnqueueAt = 0L
    private var trailingJob: Job? = null

    @Synchronized
    fun schedule() {
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
                lastEnqueueAt = System.currentTimeMillis()
                trailingJob = null
                enqueueWork()
            }
        }
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
