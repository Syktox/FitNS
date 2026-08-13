package com.raysix.fitns.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.core.model.AppError
import com.raysix.fitns.core.model.AppResult
import com.raysix.fitns.core.settings.DefaultN8nBaseUrl
import com.raysix.fitns.core.settings.N8nConnectionSettings
import com.raysix.fitns.core.sync.SyncScheduler
import com.raysix.fitns.data.local.dao.SyncQueueDao
import com.raysix.fitns.domain.model.BodyWeightLogEntry
import com.raysix.fitns.domain.model.FoodLogEntry
import com.raysix.fitns.domain.model.NutritionGoal
import com.raysix.fitns.domain.model.UserProfile
import com.raysix.fitns.domain.model.WorkoutLogEntry
import com.raysix.fitns.domain.repository.BodyWeightRepository
import com.raysix.fitns.domain.repository.N8nRepository
import com.raysix.fitns.domain.repository.NutritionRepository
import com.raysix.fitns.domain.repository.ProfileRepository
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.repository.WorkoutRepository
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val n8nSettings: N8nConnectionSettings = N8nConnectionSettings(baseUrl = DefaultN8nBaseUrl),
    val temporaryPhotosOnly: Boolean = true,
    val pendingSyncCount: Int = 0,
    val connectionStatus: String = "Not tested",
    val testingConnection: Boolean = false,
    val bearerTokenInput: String = "",
    val bearerTokenConfigured: Boolean = false,
    val exportStatus: String? = null,
    val exportPreview: String? = null
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
    moshi: Moshi
) : ViewModel() {
    private val connectionStatus = MutableStateFlow("Not tested")
    private val testingConnection = MutableStateFlow(false)
    private val bearerTokenInput = MutableStateFlow("")
    private val bearerTokenConfigured = MutableStateFlow(false)
    private val exportStatus = MutableStateFlow<String?>(null)
    private val exportPreview = MutableStateFlow<String?>(null)
    private val exportAdapter = moshi.adapter(LocalDataExport::class.java).indent("  ")
    private val connectionState = combine(connectionStatus, testingConnection, bearerTokenInput, bearerTokenConfigured) { status, testing, tokenInput, tokenConfigured ->
        ConnectionState(status = status, testing = testing, tokenInput = tokenInput, tokenConfigured = tokenConfigured)
    }
    private val exportState = combine(exportStatus, exportPreview) { status, preview ->
        status to preview
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeN8nSettings(),
        settingsRepository.observeTemporaryPhotosOnly(),
        syncQueueDao.observePendingCount(),
        connectionState,
        exportState
    ) { n8nSettings, temporaryPhotosOnly, pendingSyncCount, connection, export ->
        SettingsUiState(
            n8nSettings = n8nSettings,
            temporaryPhotosOnly = temporaryPhotosOnly,
            pendingSyncCount = pendingSyncCount,
            connectionStatus = connection.status,
            testingConnection = connection.testing,
            bearerTokenInput = connection.tokenInput,
            bearerTokenConfigured = connection.tokenConfigured,
            exportStatus = export.first,
            exportPreview = export.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            bearerTokenConfigured.value = !settingsRepository.readBearerToken().isNullOrBlank()
        }
    }

    fun updateN8nBaseUrl(baseUrl: String) {
        viewModelScope.launch {
            settingsRepository.updateN8nBaseUrl(baseUrl)
            connectionStatus.value = "Not tested"
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
                settingsRepository.clearBearerToken()
                bearerTokenConfigured.value = false
                connectionStatus.value = "Bearer token cleared"
            } else {
                settingsRepository.setBearerToken(token)
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
        syncScheduler.schedule()
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
            val json = exportAdapter.toJson(export)
            exportStatus.value = "JSON export prepared (${json.length} characters)"
            exportPreview.value = json.take(1200)
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
