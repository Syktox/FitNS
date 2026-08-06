package com.syktox.fitns.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.syktox.fitns.core.sync.SyncScheduler
import com.syktox.fitns.core.settings.DefaultN8nBaseUrl
import com.syktox.fitns.core.settings.N8nConnectionSettings
import com.syktox.fitns.core.settings.fitNsSettingsDataStore
import com.syktox.fitns.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler
) : SettingsRepository {
    override fun observeN8nSettings(): Flow<N8nConnectionSettings> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            N8nConnectionSettings(
                baseUrl = preferences[N8nBaseUrlKey] ?: DefaultN8nBaseUrl,
                bearerTokenConfigured = false,
                syncEnabled = preferences[N8nSyncEnabledKey] ?: false
            )
        }
    }

    override fun observeTemporaryPhotosOnly(): Flow<Boolean> {
        return context.fitNsSettingsDataStore.data.map { preferences ->
            preferences[TemporaryPhotosOnlyKey] ?: true
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

    private companion object {
        val N8nBaseUrlKey = stringPreferencesKey("n8n_base_url")
        val N8nSyncEnabledKey = booleanPreferencesKey("n8n_sync_enabled")
        val TemporaryPhotosOnlyKey = booleanPreferencesKey("temporary_photos_only")
    }
}
