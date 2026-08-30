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

// Compatibility names are kept because feature modules already import them. The
// palette itself follows the Blue Whale direction: deep water, clear aqua and a
// small amount of warm coral for states that need to stand apart.
val BrandGreen = Color(0xFF0B5FC0)
val BrandGreenDark = Color(0xFF7BB7FF)
val AccentLime = Color(0xFF087F7C)
val AccentLimeDark = Color(0xFF65D9D0)
val AccentAmber = Color(0xFFF2B85B)
val AccentRose = Color(0xFFF17878)
val AccentViolet = Color(0xFF4F64B8)
val Ink = Color(0xFF0B2632)
val Mist = Color(0xFFF3F8FA)
val Night = Color(0xFF041820)
val NightSurface = Color(0xFF09232E)

private val LightColors: ColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E7FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = AccentLime,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB8EFEB),
    onSecondaryContainer = Color(0xFF003735),
    tertiary = AccentViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDE2FF),
    onTertiaryContainer = Color(0xFF17275F),
    background = Mist,
    onBackground = Ink,
    surface = Color(0xFFFBFEFF),
    onSurface = Ink,
    surfaceVariant = Color(0xFFDFEAEE),
    onSurfaceVariant = Color(0xFF415F69),
    surfaceTint = BrandGreen,
    inverseSurface = Color(0xFF17343F),
    inverseOnSurface = Color(0xFFEAF7FA),
    inversePrimary = Color(0xFF7BB7FF),
    surfaceContainerLow = Color(0xFFF0F6F8),
    surfaceContainer = Color(0xFFEAF2F5),
    surfaceContainerHigh = Color(0xFFE3EDF0),
    surfaceContainerHighest = Color(0xFFDBE7EB),
    outline = Color(0xFF718B94),
    outlineVariant = Color(0xFFB9CBD1),
    scrim = Color(0xFF000000),
    error = Color(0xFFD64545),
    onError = Color.White,
    errorContainer = Color(0xFFFFE0DE),
    onErrorContainer = Color(0xFF5F0000)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = BrandGreenDark,
    onPrimary = Color(0xFF002F64),
    primaryContainer = Color(0xFF06488F),
    onPrimaryContainer = Color(0xFFD5E5FF),
    secondary = AccentLimeDark,
    onSecondary = Color(0xFF003735),
    secondaryContainer = Color(0xFF005B58),
    onSecondaryContainer = Color(0xFFA7F2EB),
    tertiary = Color(0xFFBCC5FF),
    onTertiary = Color(0xFF25346E),
    tertiaryContainer = Color(0xFF3E4C87),
    onTertiaryContainer = Color(0xFFDDE2FF),
    background = Night,
    onBackground = Color(0xFFDDEDF2),
    surface = NightSurface,
    onSurface = Color(0xFFDDEDF2),
    surfaceVariant = Color(0xFF17323D),
    onSurfaceVariant = Color(0xFFABC6CF),
    surfaceTint = BrandGreenDark,
    inverseSurface = Color(0xFFDDEDF2),
    inverseOnSurface = Color(0xFF17343F),
    inversePrimary = Color(0xFF0B5FC0),
    surfaceContainerLow = Color(0xFF0D2934),
    surfaceContainer = Color(0xFF112E39),
    surfaceContainerHigh = Color(0xFF173640),
    surfaceContainerHighest = Color(0xFF1E404B),
    outline = Color(0xFF78919A),
    outlineVariant = Color(0xFF36515B),
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
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
