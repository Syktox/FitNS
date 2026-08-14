package com.raysix.fitns.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raysix.fitns.domain.repository.BottomNavigationDestination
import com.raysix.fitns.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PrimaryNavigationViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val destinations: StateFlow<List<BottomNavigationDestination>> = settingsRepository
        .observeBottomNavigation()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BottomNavigationDestination.Default
        )
}
