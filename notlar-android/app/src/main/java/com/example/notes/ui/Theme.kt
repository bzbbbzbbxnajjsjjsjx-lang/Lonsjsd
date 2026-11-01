package com.example.notes.ui

import android.graphics.RectF
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.example.notes.data.Settings
import com.example.notes.data.ThemeMode

val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

// Baseline expressive scheme for devices without dynamic color: a deep violet seed.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF4F378B),
    secondary = Color(0xFF625B71), secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF4A4458),
    tertiary = Color(0xFF7D5260), tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF633B48),
    surface = Color(0xFFFEF7FF), surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0), surfaceContainerHighest = Color(0xFFE6E0E9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
    surface = Color(0xFF141218), surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930), surfaceContainerHighest = Color(0xFF36343B),
)

/**
 * Material 3 Expressive theme: dynamic color on Android 12+, a baseline violet scheme
 * otherwise, an optional pure-black dark mode, and the expressive spring motion scheme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesTheme(settings: Settings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    var colors: ColorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    if (dark && settings.amoled) {
        colors = colors.copy(
            background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0C0C0E), surfaceContainer = Color(0xFF121214),
            surfaceContainerHigh = Color(0xFF1A1A1D), surfaceContainerHighest = Color(0xFF232327),
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDarkTheme provides dark,
        LocalHapticsEnabled provides settings.haptics,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colors,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}

/** A note color: container and the content color that reads on it. */
data class NoteTint(val name: String, val light: Color, val dark: Color, val swatch: Color)

/** Twelve note colors. Index 0 follows the theme surface. Tones match M3 container roles. */
val NotePalette = listOf(
    NoteTint("Varsayılan", Color.Unspecified, Color.Unspecified, Color.Unspecified),
    NoteTint("Domates", Color(0xFFFFDAD6), Color(0xFF5C1A15), Color(0xFFE5553F)),
    NoteTint("Mandalina", Color(0xFFFFDBC8), Color(0xFF5A2A0C), Color(0xFFF2852E)),
    NoteTint("Kum", Color(0xFFFCE3A5), Color(0xFF4C3A00), Color(0xFFE2B33C)),
    NoteTint("Fesleğen", Color(0xFFD4EDB8), Color(0xFF213B11), Color(0xFF6FA83E)),
    NoteTint("Nane", Color(0xFFB8F0DD), Color(0xFF00382B), Color(0xFF2FB18A)),
    NoteTint("Deniz", Color(0xFFBDEBF6), Color(0xFF003641), Color(0xFF2BA4C0)),
    NoteTint("Gök", Color(0xFFD3E3FD), Color(0xFF0E2D5C), Color(0xFF4F86E8)),
    NoteTint("Lavanta", Color(0xFFE6DEFF), Color(0xFF2E2263), Color(0xFF7D67E0)),
    NoteTint("Orkide", Color(0xFFF7D9FF), Color(0xFF4A1A5A), Color(0xFFB45DD4)),
    NoteTint("Gül", Color(0xFFFFD9E3), Color(0xFF5B1530), Color(0xFFDD5A86)),
    NoteTint("Kil", Color(0xFFE9DFD7), Color(0xFF3B302A), Color(0xFF9C8576)),
)

/** Container color for a note card, or null for the theme's default card color. */
@Composable
@ReadOnlyComposable
fun noteContainer(index: Int): Color? {
    val tint = NotePalette.getOrNull(index) ?: return null
    if (index == 0) return null
    return if (LocalDarkTheme.current) tint.dark else tint.light
}

@Composable
@ReadOnlyComposable
fun noteContent(index: Int): Color =
    if (index == 0) MaterialTheme.colorScheme.onSurface
    else if (LocalDarkTheme.current) Color(0xFFF3EEF3) else Color(0xFF1D1B20)

/** Shape that morphs between two Material shape polygons; progress 0 → start, 1 → end. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = android.graphics.Path(morph.toPath(progress))
        return Outline.Generic(fit(path, size).asComposePath())
    }
}

/** A single Material shape polygon, stretched to fill the box. */
class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = android.graphics.Path(polygon.toPath())
        return Outline.Generic(fit(path, size).asComposePath())
    }
}

private fun fit(path: android.graphics.Path, size: Size): android.graphics.Path {
    val bounds = RectF()
    path.computeBounds(bounds, true)
    val matrix = android.graphics.Matrix().apply {
        setScale(size.width / bounds.width(), size.height / bounds.height())
        postTranslate(-bounds.left * size.width / bounds.width(), -bounds.top * size.height / bounds.height())
    }
    path.transform(matrix)
    return path
}
