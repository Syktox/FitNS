package com.syktox.fitns.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandGreen = Color(0xFF16A34A)
val BrandGreenDark = Color(0xFF4ADE80)
val AccentLime = Color(0xFF84CC16)
val AccentAmber = Color(0xFFF59E0B)
val AccentRose = Color(0xFFF43F5E)
val Ink = Color(0xFF0F172A)
val Mist = Color(0xFFF1F5F9)
val Night = Color(0xFF0B1220)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F2D5),
    onPrimaryContainer = Color(0xFF0C3D1E),
    secondary = AccentLime,
    onSecondary = Color(0xFF1A2E00),
    secondaryContainer = Color(0xFFE7F9C4),
    onSecondaryContainer = Color(0xFF2C4700),
    tertiary = AccentRose,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8DD),
    onTertiaryContainer = Color(0xFF560013),
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color(0xFF063D1A),
    primaryContainer = Color(0xFF0E5C2A),
    onPrimaryContainer = Color(0xFFC9F2D5),
    secondary = AccentLime,
    onSecondary = Color(0xFF1A2E00),
    secondaryContainer = Color(0xFF365500),
    onSecondaryContainer = Color(0xFFE7F9C4),
    tertiary = Color(0xFFFFB1C0),
    onTertiary = Color(0xFF560013),
    tertiaryContainer = Color(0xFF7A2A3C),
    onTertiaryContainer = Color(0xFFFFD8DD),
    background = Night,
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111A2C),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF22304A),
    onSurfaceVariant = Color(0xFFA7B4C8),
    surfaceContainerHighest = Color(0xFF22304A),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = Typography().bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = Typography().labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelSmall = Typography().labelSmall.copy(fontFamily = FontFamily.SansSerif, letterSpacing = 0.6.sp)
)

@Composable
fun FitNsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
