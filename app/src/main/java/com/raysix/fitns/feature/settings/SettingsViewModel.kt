package com.raysix.fitns.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.auth.GoogleAuthController
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.settings.DefaultN8nBaseUrl
import com.raysix.fitns.core.settings.N8nConnectionSettings
import com.raysix.fitns.core.sync.SyncScheduler
import com.raysix.fitns.core.network.N8nServiceFactory
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.model.GoogleAccount
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.BottomNavigationDestination
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val n8nSettings: N8nConnectionSettings = N8nConnectionSettings(baseUrl = DefaultN8nBaseUrl),
    val temporaryPhotosOnly: Boolean = true,
    val mealPhotoAnalysisEnabled: Boolean = false,
    val pendingSyncCount: Int = 0,
    val failedSyncCount: Int = 0,
    val latestSyncError: String? = null,
    val connectionStatus: String = "Not tested",
    val testingConnection: Boolean = false,
    val bearerTokenInput: String = "",
    val bearerTokenConfigured: Boolean = false,
    val googleAccount: GoogleAccount? = null,
    val googleSignInConfigured: Boolean = false,
    val appearanceMode: AppearanceMode = AppearanceMode.System,
    val bottomNavigation: List<BottomNavigationDestination> = BottomNavigationDestination.Default,
    val exportStatus: String? = null,
    val exportFilePath: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val n8nRepository: N8nRepository,
    private val syncQueueDao: SyncQueueDao,
    private val syncScheduler: SyncScheduler,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
    private val bodyWeightRepository: BodyWeightRepository,
    private val profileRepository: ProfileRepository,
    private val googleAuthController: GoogleAuthController,
    private val n8nServiceFactory: N8nServiceFactory,
    @ApplicationContext private val context: Context,
    moshi: Moshi
) : ViewModel() {
    private val connectionStatus = MutableStateFlow("Not tested")
    private val testingConnection = MutableStateFlow(false)
    private val bearerTokenInput = MutableStateFlow("")
    private val bearerTokenConfigured = MutableStateFlow(false)
    private val exportStatus = MutableStateFlow<String?>(null)
    private val exportFilePath = MutableStateFlow<String?>(null)
    private val baseUrlDraft = MutableStateFlow(DefaultN8nBaseUrl)
    private val navigationUpdateMutex = Mutex()
    private val exportAdapter = moshi.adapter(LocalDataExport::class.java).indent("  ")
    private val connectionState = combine(connectionStatus, testingConnection, bearerTokenInput, bearerTokenConfigured) { status, testing, tokenInput, tokenConfigured ->
        ConnectionState(status = status, testing = testing, tokenInput = tokenInput, tokenConfigured = tokenConfigured)
    }
    private val exportState = combine(exportStatus, exportFilePath) { status, filePath ->
        status to filePath
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeN8nSettings(),
        settingsRepository.observeTemporaryPhotosOnly(),
        syncQueueDao.observePendingCount(),
        connectionState,
        exportState,
        settingsRepository.observeGoogleAccount(),
        settingsRepository.observeAppearanceMode(),
        baseUrlDraft,
        syncQueueDao.observeConflictCount(),
        syncQueueDao.observeLatestConflictError(),
        settingsRepository.observeBottomNavigation(),
        settingsRepository.observeMealPhotoAnalysisEnabled()
    ) { values ->
        val n8nSettings = values[0] as N8nConnectionSettings
        val temporaryPhotosOnly = values[1] as Boolean
        val pendingSyncCount = values[2] as Int
        val connection = values[3] as ConnectionState
        @Suppress("UNCHECKED_CAST")
        val export = values[4] as Pair<String?, String?>
        @Suppress("UNCHECKED_CAST")
        val bottomNavigation = values[10] as List<BottomNavigationDestination>
        SettingsUiState(
            n8nSettings = n8nSettings.copy(baseUrl = values[7] as String),
            temporaryPhotosOnly = temporaryPhotosOnly,
            mealPhotoAnalysisEnabled = values[11] as Boolean,
            pendingSyncCount = pendingSyncCount,
            failedSyncCount = values[8] as Int,
            latestSyncError = values[9] as String?,
            connectionStatus = connection.status,
            testingConnection = connection.testing,
            bearerTokenInput = connection.tokenInput,
            bearerTokenConfigured = connection.tokenConfigured,
            googleAccount = values[5] as GoogleAccount?,
            googleSignInConfigured = googleAuthController.isConfigured,
            appearanceMode = values[6] as AppearanceMode,
            bottomNavigation = bottomNavigation,
            exportStatus = export.first,
            exportFilePath = export.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            bearerTokenConfigured.value = !settingsRepository.readBearerToken().isNullOrBlank()
            baseUrlDraft.value = settingsRepository.observeN8nSettings().first().baseUrl
        }
    }

    fun updateN8nBaseUrl(baseUrl: String) {
        baseUrlDraft.value = baseUrl
        connectionStatus.value = "Not tested"
    }

    fun saveN8nBaseUrl() {
        viewModelScope.launch {
            val url = n8nServiceFactory.normalizeBaseUrl(baseUrlDraft.value)
            if (url == null) {
                connectionStatus.value = "Enter a valid HTTPS base URL."
                return@launch
            }
            settingsRepository.updateN8nBaseUrl(url)
            syncQueueDao.requeueConfigurationFailures(System.currentTimeMillis())
            connectionStatus.value = "Connection address saved. Test it before syncing."
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSyncEnabled(enabled)
        }
    }

    fun updateBearerToken(token: String) {
        bearerTokenInput.value = token
    }

    fun saveBearerToken() {
        viewModelScope.launch {
            val token = bearerTokenInput.value.trim()
            if (token.isBlank()) {
                connectionStatus.value = if (bearerTokenConfigured.value) {
                    "Enter a replacement token to update the stored credential."
                } else {
                    "Enter a bearer token first."
                }
            } else {
                settingsRepository.setBearerToken(token)
                syncQueueDao.requeueConfigurationFailures(System.currentTimeMillis())
                bearerTokenConfigured.value = true
                bearerTokenInput.value = ""
                connectionStatus.value = "Bearer token saved securely"
            }
        }
    }

    fun updateTemporaryPhotosOnly(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTemporaryPhotosOnly(enabled)
        }
    }

    fun updateMealPhotoAnalysisEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMealPhotoAnalysisEnabled(enabled)
        }
    }

    fun updateAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch { settingsRepository.updateAppearanceMode(mode) }
    }

    fun updateBottomNavigation(destination: BottomNavigationDestination, selected: Boolean) {
        viewModelScope.launch {
            navigationUpdateMutex.withLock {
                val current = settingsRepository.observeBottomNavigation().first()
                val updated = when {
                    selected && destination !in current && current.size < BottomNavigationDestination.MaxSelected -> current + destination
                    !selected && destination in current && current.size > 1 -> current - destination
                    else -> current
                }
                if (updated != current) {
                    settingsRepository.updateBottomNavigation(updated)
                }
            }
        }
    }

    fun resetBottomNavigation() {
        viewModelScope.launch {
            navigationUpdateMutex.withLock {
                settingsRepository.updateBottomNavigation(BottomNavigationDestination.Default)
            }
        }
    }

    fun createSignInIntent(): Intent? = googleAuthController.createSignInIntent()

    fun handleSignInResult(data: Intent?): Boolean {
        val account = googleAuthController.handleSignInResult(data) ?: return false
        viewModelScope.launch { settingsRepository.saveGoogleAccount(account) }
        return true
    }

    fun signOut() {
        googleAuthController.signOut()
        viewModelScope.launch { settingsRepository.clearGoogleAccount() }
    }

    fun testConnection() {
        viewModelScope.launch {
            testingConnection.value = true
            connectionStatus.value = "Testing connection..."
            val result = n8nRepository.testConnection(
                baseUrl = uiState.value.n8nSettings.baseUrl,
                bearerToken = settingsRepository.readBearerToken()
            )
            connectionStatus.value = when (result) {
                is AppResult.Success -> "Connection successful"
                is AppResult.Failure -> result.error.toMessage()
            }
            testingConnection.value = false
        }
    }

    fun retrySyncNow() {
        viewModelScope.launch {
            syncQueueDao.requeueConfigurationFailures(System.currentTimeMillis())
            syncScheduler.schedule()
        }
    }

    fun generateLocalJsonExport() {
        viewModelScope.launch {
            val export = LocalDataExport(
                generatedAt = System.currentTimeMillis(),
                profile = profileRepository.observeProfile().first(),
                nutritionGoal = profileRepository.observeNutritionGoal().first(),
                foodEntries = nutritionRepository.observeFoodHistory().first(),
                workouts = workoutRepository.observeHistory().first(),
                bodyWeights = bodyWeightRepository.observeHistory().first()
            )
            val file = withContext(Dispatchers.IO) {
                val json = exportAdapter.toJson(export)
                File(context.cacheDir, "fitns-export-${System.currentTimeMillis()}.json").apply {
                    writeText(json)
                }
            }
            exportStatus.value = "Your export is ready to share."
            exportFilePath.value = file.absolutePath
        }
    }

    private fun AppError.toMessage(): String {
        return when (this) {
            AppError.Offline -> "n8n is unreachable or there is no connection."
            AppError.Timeout -> "Connection test timed out."
            AppError.Unauthorized -> "Authentication failed."
            AppError.NotFound -> "Health webhook was not found."
            is AppError.Remote -> "Server error during health check: ${code ?: "unknown"}"
            is AppError.Validation -> message
            is AppError.Unknown -> message
        }
    }
}

private data class ConnectionState(
    val status: String,
    val testing: Boolean,
    val tokenInput: String,
    val tokenConfigured: Boolean
)

data class LocalDataExport(
    val generatedAt: Long,
    val profile: UserProfile,
    val nutritionGoal: NutritionGoal,
    val foodEntries: List<FoodLogEntry>,
    val workouts: List<WorkoutLogEntry>,
    val bodyWeights: List<BodyWeightLogEntry>
)
