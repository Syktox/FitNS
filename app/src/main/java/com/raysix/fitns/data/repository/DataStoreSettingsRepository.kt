package com.raysix.fitns.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.raysix.fitns.core.settings.EncryptedTokenStore
import com.raysix.fitns.core.sync.SyncScheduler
import com.raysix.fitns.core.settings.DefaultN8nBaseUrl
import com.raysix.fitns.core.settings.N8nConnectionSettings
import com.raysix.fitns.core.settings.fitNsSettingsDataStore
import com.raysix.fitns.core.undo.AppUndoRedoManager
import com.raysix.fitns.core.undo.UndoRedoAction
import com.raysix.fitns.domain.model.GoogleAccount
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.domain.repository.BottomNavigationDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler,
    private val tokenStore: EncryptedTokenStore,
    private val undoRedoManager: AppUndoRedoManager
) : SettingsRepository {
    override fun observeN8nSettings(): Flow<N8nConnectionSettings> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            N8nConnectionSettings(
                baseUrl = preferences[N8nBaseUrlKey] ?: DefaultN8nBaseUrl,
                bearerTokenConfigured = tokenStore.hasToken(),
                syncEnabled = preferences[N8nSyncEnabledKey] ?: false
            )
        }
    }

    override fun observeTemporaryPhotosOnly(): Flow<Boolean> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[TemporaryPhotosOnlyKey] ?: true
        }
    }

    override fun observeMealPhotoAnalysisEnabled(): Flow<Boolean> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[MealPhotoAnalysisEnabledKey] ?: false
        }
    }

    override fun observeOnboardingCompleted(): Flow<Boolean> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[OnboardingCompletedKey] ?: false
        }
    }

    override fun observeGoogleAccount(): Flow<GoogleAccount?> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[GoogleAccountEmailKey]?.let { email ->
                GoogleAccount(
                    email = email,
                    displayName = preferences[GoogleAccountNameKey] ?: "",
                    photoUrl = preferences[GoogleAccountPhotoUrlKey]
                )
            }
        }
    }

    override fun observeAppearanceMode(): Flow<AppearanceMode> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[AppearanceModeKey]
                ?.let { stored -> AppearanceMode.entries.firstOrNull { it.name == stored } }
                ?: AppearanceMode.System
        }
    }

    override fun observeBottomNavigation(): Flow<List<BottomNavigationDestination>> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[BottomNavigationKey]
                ?.split(",")
                ?.mapNotNull { stored ->
                    BottomNavigationDestination.entries.firstOrNull { it.name == stored }
                }
                ?.distinct()
                ?.take(BottomNavigationDestination.MaxSelected)
                ?.takeIf { it.isNotEmpty() }
                ?: BottomNavigationDestination.Default
        }
    }

    override suspend fun updateN8nBaseUrl(baseUrl: String) {
        val previous = observeN8nSettings().first().baseUrl
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[N8nBaseUrlKey] = baseUrl
        }
        recordUndo(
            label = "n8n URL",
            undo = { updateN8nBaseUrl(previous) },
            redo = { updateN8nBaseUrl(baseUrl) }
        )
    }

    override suspend fun updateSyncEnabled(enabled: Boolean) {
        val previous = observeN8nSettings().first().syncEnabled
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[N8nSyncEnabledKey] = enabled
        }
        if (enabled) {
            syncScheduler.schedule()
        }
        recordUndo(
            label = "sync setting",
            undo = { updateSyncEnabled(previous) },
            redo = { updateSyncEnabled(enabled) }
        )
    }

    override suspend fun updateTemporaryPhotosOnly(enabled: Boolean) {
        val previous = observeTemporaryPhotosOnly().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[TemporaryPhotosOnlyKey] = enabled
        }
        recordUndo(
            label = "photo privacy setting",
            undo = { updateTemporaryPhotosOnly(previous) },
            redo = { updateTemporaryPhotosOnly(enabled) }
        )
    }

    override suspend fun updateMealPhotoAnalysisEnabled(enabled: Boolean) {
        val previous = observeMealPhotoAnalysisEnabled().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[MealPhotoAnalysisEnabledKey] = enabled
        }
        recordUndo(
            label = "meal photo analysis setting",
            undo = { updateMealPhotoAnalysisEnabled(previous) },
            redo = { updateMealPhotoAnalysisEnabled(enabled) }
        )
    }

    override suspend fun completeOnboarding() {
        val previous = observeOnboardingCompleted().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[OnboardingCompletedKey] = true
        }
        recordUndo(
            label = "onboarding",
            undo = { updateOnboardingCompleted(previous) },
            redo = { completeOnboarding() }
        )
    }

    override suspend fun saveGoogleAccount(account: GoogleAccount) {
        val previous = observeGoogleAccount().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[GoogleAccountEmailKey] = account.email
            preferences[GoogleAccountNameKey] = account.displayName
            account.photoUrl?.let { preferences[GoogleAccountPhotoUrlKey] = it }
        }
        recordUndo(
            label = "Google account",
            undo = {
                if (previous == null) {
                    clearGoogleAccount()
                } else {
                    saveGoogleAccount(previous)
                }
            },
            redo = { saveGoogleAccount(account) }
        )
    }

    override suspend fun clearGoogleAccount() {
        val previous = observeGoogleAccount().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences.remove(GoogleAccountEmailKey)
            preferences.remove(GoogleAccountNameKey)
            preferences.remove(GoogleAccountPhotoUrlKey)
        }
        previous?.let { account ->
            recordUndo(
                label = "Google account removal",
                undo = { saveGoogleAccount(account) },
                redo = { clearGoogleAccount() }
            )
        }
    }

    override suspend fun updateAppearanceMode(mode: AppearanceMode) {
        val previous = observeAppearanceMode().first()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[AppearanceModeKey] = mode.name
        }
        recordUndo(
            label = "appearance",
            undo = { updateAppearanceMode(previous) },
            redo = { updateAppearanceMode(mode) }
        )
    }

    override suspend fun updateBottomNavigation(destinations: List<BottomNavigationDestination>) {
        val previous = observeBottomNavigation().first()
        val sanitized = destinations
            .distinct()
            .take(BottomNavigationDestination.MaxSelected)
            .ifEmpty { BottomNavigationDestination.Default }
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[BottomNavigationKey] = sanitized.joinToString(",") { it.name }
        }
        recordUndo(
            label = "bottom navigation",
            undo = { updateBottomNavigation(previous) },
            redo = { updateBottomNavigation(sanitized) }
        )
    }

    override suspend fun setBearerToken(token: String) {
        val previous = tokenStore.read()
        tokenStore.save(token.trim())
        recordUndo(
            label = "bearer token",
            undo = {
                if (previous == null) {
                    clearBearerToken()
                } else {
                    setBearerToken(previous)
                }
            },
            redo = { setBearerToken(token) }
        )
    }

    override suspend fun readBearerToken(): String? = tokenStore.read()

    override suspend fun clearBearerToken() {
        val previous = tokenStore.read()
        tokenStore.clear()
        previous?.let { token ->
            recordUndo(
                label = "bearer token removal",
                undo = { setBearerToken(token) },
                redo = { clearBearerToken() }
            )
        }
    }

    override suspend fun clearAllLocalSettings() {
        // Remove the credential first. If clearing DataStore then fails, the
        // deletion flow reports a failure and can safely be retried without
        // leaving a usable bearer token behind.
        tokenStore.clear()
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences.clear()
        }
        undoRedoManager.clear("Local data was deleted.")
    }

    private suspend fun updateOnboardingCompleted(completed: Boolean) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[OnboardingCompletedKey] = completed
        }
    }

    private fun recordUndo(label: String, undo: suspend () -> Unit, redo: suspend () -> Unit) {
        undoRedoManager.record(UndoRedoAction(label = label, undo = undo, redo = redo))
    }

    private companion object {
        val N8nBaseUrlKey = stringPreferencesKey("n8n_base_url")
        val N8nSyncEnabledKey = booleanPreferencesKey("n8n_sync_enabled")
        val TemporaryPhotosOnlyKey = booleanPreferencesKey("temporary_photos_only")
        val MealPhotoAnalysisEnabledKey = booleanPreferencesKey("meal_photo_analysis_enabled")
        val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
        val GoogleAccountEmailKey = stringPreferencesKey("google_account_email")
        val GoogleAccountNameKey = stringPreferencesKey("google_account_name")
        val GoogleAccountPhotoUrlKey = stringPreferencesKey("google_account_photo_url")
        val AppearanceModeKey = stringPreferencesKey("appearance_mode")
        val BottomNavigationKey = stringPreferencesKey("bottom_navigation")
    }
}
