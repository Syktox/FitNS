package com.raysix.fitns.data.repository

import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDataDeletionCoordinatorTest {

    @Test
    fun deleteAllLocalDataRunsEveryStepInSafeOrderAndResumesSync() = runTest {
        val calls = mutableListOf<String>()
        val coordinator = LocalDataDeletionCoordinator(
            pauseSync = { calls += "pause sync" },
            resumeSync = { calls += "resume sync" },
            deletionSteps = listOf(
                LocalDataDeletionStep("database") { calls += "database" },
                LocalDataDeletionStep("cache") { calls += "cache" },
                LocalDataDeletionStep("settings") { calls += "settings" }
            )
        )

        val result = coordinator.deleteAllLocalData()

        assertTrue(result is AppResult.Success)
        assertEquals(
            listOf("pause sync", "database", "cache", "settings", "resume sync"),
            calls
        )
    }

    @Test
    fun deleteAllLocalDataStopsAfterFailureReportsStageAndDoesNotReturnSuccess() = runTest {
        val calls = mutableListOf<String>()
        val reportedFailures = mutableListOf<String>()
        val coordinator = LocalDataDeletionCoordinator(
            pauseSync = { calls += "pause sync" },
            resumeSync = { calls += "resume sync" },
            deletionSteps = listOf(
                LocalDataDeletionStep("database") { calls += "database" },
                LocalDataDeletionStep("cache") {
                    calls += "cache"
                    throw IOException("disk failure")
                },
                LocalDataDeletionStep("settings") { calls += "settings" }
            ),
            onFailure = { stage, _ -> reportedFailures += stage }
        )

        val result = coordinator.deleteAllLocalData()

        assertTrue(result is AppResult.Failure)
        val error = (result as AppResult.Failure).error
        assertTrue(error is AppError.Unknown && error.message.contains("cache"))
        assertEquals(listOf("pause sync", "database", "cache", "resume sync"), calls)
        assertEquals(listOf("cache"), reportedFailures)
    }
}
