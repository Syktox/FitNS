package com.raysix.fitns.core.design

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppAppearanceViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val appearanceMode: StateFlow<AppearanceMode> = settingsRepository.observeAppearanceMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceMode.System)
}
