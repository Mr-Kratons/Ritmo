package com.example.ritmo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.ritmo.data.ThemeMode

// Âmbar sobre grafite: o único tom vivo do app. As cores do player vêm da capa do álbum.
val Amber = Color(0xFFFFB020)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1B1303),
    primaryContainer = Color(0xFF3A2A06),
    onPrimaryContainer = Color(0xFFFFDDA0),
    secondary = Color(0xFFFF8A4C),
    onSecondary = Color(0xFF2A1100),
    background = Color(0xFF0C0D12),
    onBackground = Color(0xFFF3F0EA),
    surface = Color(0xFF0C0D12),
    onSurface = Color(0xFFF3F0EA),
    surfaceVariant = Color(0xFF1C1F28),
    onSurfaceVariant = Color(0xFF9B9AA6),
    outline = Color(0xFF3A3E4B),
    outlineVariant = Color(0xFF262A35),
    error = Color(0xFFFF6B6B),
    inverseSurface = Color(0xFFF3F0EA),
    inverseOnSurface = Color(0xFF15161C),
    surfaceContainerLowest = Color(0xFF08090D),
    surfaceContainerLow = Color(0xFF12141A),
    surfaceContainer = Color(0xFF15171F),
    surfaceContainerHigh = Color(0xFF1C1F28),
    surfaceContainerHighest = Color(0xFF242835),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC77700),
    onPrimary = Color(0xFF1B1303),
    primaryContainer = Color(0xFFFFE2B0),
    onPrimaryContainer = Color(0xFF2D1B00),
    secondary = Color(0xFFD1521B),
    background = Color(0xFFF6F6F8),
    onBackground = Color(0xFF15161C),
    surface = Color(0xFFF6F6F8),
    onSurface = Color(0xFF15161C),
    surfaceVariant = Color(0xFFE9E9EE),
    onSurfaceVariant = Color(0xFF666773),
    outline = Color(0xFFB9BAC4),
    outlineVariant = Color(0xFFDADBE2),
    error = Color(0xFFC62828),
    inverseSurface = Color(0xFF15161C),
    inverseOnSurface = Color(0xFFF3F0EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F1F5),
    surfaceContainer = Color(0xFFEDEDF2),
    surfaceContainerHigh = Color(0xFFE6E6EC),
    surfaceContainerHighest = Color(0xFFDFDFE6),
)

private val Sans = FontFamily.SansSerif

val RitmoTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 27.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

val RitmoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun RitmoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = RitmoTypography,
        shapes = RitmoShapes,
        content = content,
    )
}
