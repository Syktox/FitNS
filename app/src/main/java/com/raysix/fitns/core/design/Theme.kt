package com.raysix.fitns.core.design

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val BrandGreen = Color(0xFF007A45)
val BrandGreenDark = Color(0xFF3BEF8F)
val AccentLime = Color(0xFFA8E10C)
val AccentLimeDark = Color(0xFFC8E64D)
val AccentAmber = Color(0xFFFFB020)
val AccentRose = Color(0xFFFF5A5F)
val AccentViolet = Color(0xFF8B6DF5)
val Ink = Color(0xFF0B1210)
val Mist = Color(0xFFF3F6F2)
val Night = Color(0xFF0A0E0C)
val NightSurface = Color(0xFF111612)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF5D1),
    onPrimaryContainer = Color(0xFF00331C),
    secondary = Color(0xFF4E7A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF3B4),
    onSecondaryContainer = Color(0xFF2C4300),
    tertiary = AccentViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5DFFF),
    onTertiaryContainer = Color(0xFF28134F),
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EDE6),
    onSurfaceVariant = Color(0xFF4A5B4F),
    surfaceTint = BrandGreen,
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEEF2EC),
    inversePrimary = Color(0xFFA0EFBC),
    surfaceContainerLow = Color(0xFFF0F3EE),
    surfaceContainer = Color(0xFFE9EDE7),
    surfaceContainerHigh = Color(0xFFE3E8E1),
    surfaceContainerHighest = Color(0xFFDDE3DB),
    outline = Color(0xFF89948A),
    outlineVariant = Color(0xFFC2CCC2),
    scrim = Color(0xFF000000),
    error = Color(0xFFD64545),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DE),
    onErrorContainer = Color(0xFF5F0000)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color(0xFF00331C),
    primaryContainer = Color(0xFF00623A),
    onPrimaryContainer = Color(0xFFBFF5D1),
    secondary = AccentLimeDark,
    onSecondary = Color(0xFF283600),
    secondaryContainer = Color(0xFF3C4D00),
    onSecondaryContainer = Color(0xFFE1F3AE),
    tertiary = Color(0xFFB9A6FF),
    onTertiary = Color(0xFF3A2270),
    tertiaryContainer = Color(0xFF4A3188),
    onTertiaryContainer = Color(0xFFE5DFFF),
    background = Night,
    onBackground = Color(0xFFE6ECE5),
    surface = NightSurface,
    onSurface = Color(0xFFE6ECE5),
    surfaceVariant = Color(0xFF222A24),
    onSurfaceVariant = Color(0xFFB4C2B6),
    surfaceTint = BrandGreenDark,
    inverseSurface = Color(0xFFE6ECE5),
    inverseOnSurface = Color(0xFF2C322E),
    inversePrimary = Color(0xFF00623A),
    surfaceContainerLow = Color(0xFF151A17),
    surfaceContainer = Color(0xFF1A211C),
    surfaceContainerHigh = Color(0xFF222B24),
    surfaceContainerHighest = Color(0xFF2C372E),
    outline = Color(0xFF7C8A7E),
    outlineVariant = Color(0xFF3D483F),
    scrim = Color(0xFF000000),
    error = Color(0xFFFF8B8B),
    onError = Color(0xFF690000),
    errorContainer = Color(0xFF9A2F2F),
    onErrorContainer = Color(0xFFFFE0DE)
)

private val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = Typography().displayMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun FitNsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
