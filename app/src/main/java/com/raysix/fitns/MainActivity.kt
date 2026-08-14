package com.raysix.fitns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raysix.fitns.core.design.AppAppearanceViewModel
import com.raysix.fitns.core.design.FitNsTheme
import com.raysix.fitns.domain.repository.AppearanceMode
import com.raysix.fitns.navigation.FitNsApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appearanceViewModel: AppAppearanceViewModel = hiltViewModel()
            val mode = appearanceViewModel.appearanceMode.collectAsStateWithLifecycle().value
            val darkTheme = when (mode) {
                AppearanceMode.System -> isSystemInDarkTheme()
                AppearanceMode.Light -> false
                AppearanceMode.Dark -> true
            }
            FitNsTheme(darkTheme = darkTheme) {
                FitNsApp()
            }
        }
    }
}
