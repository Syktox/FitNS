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
import com.raysix.fitns.domain.model.GoogleAccount
import com.raysix.fitns.domain.repository.SettingsRepository
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.domain.repository.BottomNavigationDestination
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler,
    private val tokenStore: EncryptedTokenStore
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
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[N8nBaseUrlKey] = baseUrl
        }
    }

    override suspend fun updateSyncEnabled(enabled: Boolean) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[N8nSyncEnabledKey] = enabled
        }
        if (enabled) {
            syncScheduler.schedule()
        }
    }

    override suspend fun updateTemporaryPhotosOnly(enabled: Boolean) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[TemporaryPhotosOnlyKey] = enabled
        }
    }

    override suspend fun updateMealPhotoAnalysisEnabled(enabled: Boolean) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[MealPhotoAnalysisEnabledKey] = enabled
        }
    }

    override suspend fun completeOnboarding() {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[OnboardingCompletedKey] = true
        }
    }

    override suspend fun saveGoogleAccount(account: GoogleAccount) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[GoogleAccountEmailKey] = account.email
            preferences[GoogleAccountNameKey] = account.displayName
            account.photoUrl?.let { preferences[GoogleAccountPhotoUrlKey] = it }
        }
    }

    override suspend fun clearGoogleAccount() {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences.remove(GoogleAccountEmailKey)
            preferences.remove(GoogleAccountNameKey)
            preferences.remove(GoogleAccountPhotoUrlKey)
        }
    }

    override suspend fun updateAppearanceMode(mode: AppearanceMode) {
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[AppearanceModeKey] = mode.name
        }
    }

    override suspend fun updateBottomNavigation(destinations: List<BottomNavigationDestination>) {
        val sanitized = destinations
            .distinct()
            .take(BottomNavigationDestination.MaxSelected)
            .ifEmpty { BottomNavigationDestination.Default }
        context.fitNsSettingsDataStore.edit { preferences ->
            preferences[BottomNavigationKey] = sanitized.joinToString(",") { it.name }
        }
    }

    override suspend fun setBearerToken(token: String) {
        tokenStore.save(token.trim())
    }

    override suspend fun readBearerToken(): String? = tokenStore.read()

    override suspend fun clearBearerToken() {
        tokenStore.clear()
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
