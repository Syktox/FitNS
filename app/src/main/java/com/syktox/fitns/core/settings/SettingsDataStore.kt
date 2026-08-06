package com.syktox.fitns.core.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.fitNsSettingsDataStore by preferencesDataStore(name = "fitns_settings")

