package com.raysix.fitns.data.repository

import android.util.Log
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.sync.SyncScheduler
import com.raysix.fitns.data.local.FitNsCacheManager
import com.raysix.fitns.data.local.FitNsDatabase
import com.raysix.fitns.domain.repository.LocalDataDeletionRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultLocalDataDeletionRepository @Inject constructor(
    private val database: FitNsDatabase,
    private val settingsRepository: SettingsRepository,
    private val syncScheduler: SyncScheduler,
    private val cacheManager: FitNsCacheManager
) : LocalDataDeletionRepository {

    private val coordinator = LocalDataDeletionCoordinator(
        pauseSync = syncScheduler::pauseAndCancelPendingWork,
        resumeSync = syncScheduler::resumeScheduling,
        deletionSteps = listOf(
            LocalDataDeletionStep("the local database", database::clearAllTables),
            LocalDataDeletionStep(
                "FitNS captures and exports",
                cacheManager::deleteFitNsCacheFiles
            ),
            LocalDataDeletionStep(
                "local settings and credentials",
                settingsRepository::clearAllLocalSettings
            )
        ),
        onFailure = { stage, exception ->
            Log.e(Tag, "Local data deletion failed while removing $stage", exception)
        }
    )

    override suspend fun deleteAllLocalData(): AppResult<Unit> = withContext(Dispatchers.IO) {
        coordinator.deleteAllLocalData()
    }

    private companion object {
        const val Tag = "FitNsDataDeletion"
    }
}

internal data class LocalDataDeletionStep(
    val description: String,
    val delete: suspend () -> Unit
)

/** Coordinates heterogeneous stores while preserving retryable failure semantics. */
internal class LocalDataDeletionCoordinator(
    private val pauseSync: suspend () -> Unit,
    private val resumeSync: () -> Unit,
    private val deletionSteps: List<LocalDataDeletionStep>,
    private val onFailure: (stage: String, exception: Exception) -> Unit = { _, _ -> }
) {
    suspend fun deleteAllLocalData(): AppResult<Unit> {
        var stage = PendingSyncStage
        var syncPaused = false
        return try {
            // Work must be stopped before the outbox tables are cleared, otherwise
            // an already-running worker could race the deletion transaction.
            pauseSync()
            syncPaused = true

            deletionSteps.forEach { deletionStep ->
                stage = deletionStep.description
                deletionStep.delete()
            }

            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            onFailure(stage, exception)
            AppResult.Failure(
                AppError.Unknown("Could not remove $stage. Please try again.")
            )
        } finally {
            if (syncPaused) {
                resumeSync()
            }
        }
    }

    private companion object {
        const val PendingSyncStage = "pending sync work"
    }
}
