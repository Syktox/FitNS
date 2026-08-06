package com.syktox.fitns.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF1D6F62),
    onPrimary = Color.White,
    secondary = Color(0xFF6D5E2E),
    onSecondary = Color.White,
    tertiary = Color(0xFF8B3F5B),
    background = Color(0xFFF7F9F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE0E7E2),
    error = Color(0xFFB3261E)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8DD8C8),
    secondary = Color(0xFFD9C987),
    tertiary = Color(0xFFFFB0C8),
    background = Color(0xFF111412),
    surface = Color(0xFF191D1A),
    surfaceVariant = Color(0xFF3F4944),
    error = Color(0xFFFFB4AB)
)

@Composable
fun FitNsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}

