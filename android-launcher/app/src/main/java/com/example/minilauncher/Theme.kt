package com.example.minilauncher

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Accent colours for the switches, the calculator card and the icons.
 * DYNAMIC follows the wallpaper on Android 12 and newer, the others are fixed.
 */
enum class AccentColor(
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightOnContainer: Color,
    val darkOnContainer: Color,
) {
    DYNAMIC(
        Color.Unspecified, Color.Unspecified, Color.Unspecified,
        Color.Unspecified, Color.Unspecified, Color.Unspecified,
    ),
    BLUE(
        Color(0xFF1E5BB8), Color(0xFFA8C8FF), Color(0xFFD6E3FF),
        Color(0xFF224A7A), Color(0xFF001B3D), Color(0xFFD6E3FF),
    ),
    GREEN(
        Color(0xFF2E6B2E), Color(0xFFA0D996), Color(0xFFC4F0BC),
        Color(0xFF1F4D1F), Color(0xFF0C2A0C), Color(0xFFC4F0BC),
    ),
    PURPLE(
        Color(0xFF6A3FB0), Color(0xFFD3BCFF), Color(0xFFEADDFF),
        Color(0xFF4F2F8A), Color(0xFF24005B), Color(0xFFEADDFF),
    ),
    ORANGE(
        Color(0xFFA34A00), Color(0xFFFFB68A), Color(0xFFFFDBC6),
        Color(0xFF7A3300), Color(0xFF351000), Color(0xFFFFDBC6),
    ),
    PINK(
        Color(0xFFA2285F), Color(0xFFFFAFD2), Color(0xFFFFD8EA),
        Color(0xFF7A1F4A), Color(0xFF3E0020), Color(0xFFFFD8EA),
    ),
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MiniLauncherTheme(
    themeMode: ThemeMode,
    accent: AccentColor,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val base: ColorScheme = when {
        accent == AccentColor.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val colors = if (accent == AccentColor.DYNAMIC) {
        base
    } else {
        base.copy(
            primary = if (dark) accent.darkPrimary else accent.lightPrimary,
            onPrimary = if (dark) Color(0xFF101010) else Color.White,
            secondaryContainer = if (dark) accent.darkContainer else accent.lightContainer,
            onSecondaryContainer = if (dark) accent.darkOnContainer else accent.lightOnContainer,
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
