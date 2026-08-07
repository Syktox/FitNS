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
        val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
        val GoogleAccountEmailKey = stringPreferencesKey("google_account_email")
        val GoogleAccountNameKey = stringPreferencesKey("google_account_name")
        val GoogleAccountPhotoUrlKey = stringPreferencesKey("google_account_photo_url")
    }
}
