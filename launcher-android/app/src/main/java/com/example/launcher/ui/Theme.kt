package com.example.launcher.ui

import android.graphics.RectF
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.example.launcher.Settings
import com.example.launcher.ThemeMode

val LocalHapticsEnabled = staticCompositionLocalOf { true }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LauncherTheme(settings: Settings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (settings.themeMode) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colors = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    CompositionLocalProvider(LocalHapticsEnabled provides settings.haptics) {
        MaterialExpressiveTheme(colorScheme = colors, motionScheme = MotionScheme.expressive(), content = content)
    }
}

class Haptics(private val perform: (HapticFeedbackType) -> Unit, private val enabled: Boolean) {
    fun tick() = fire(HapticFeedbackType.ContextClick)
    fun confirm() = fire(HapticFeedbackType.Confirm)
    fun long() = fire(HapticFeedbackType.LongPress)
    fun segment() = fire(HapticFeedbackType.SegmentTick)
    fun toggle(on: Boolean) = fire(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
    private fun fire(type: HapticFeedbackType) { if (enabled) perform(type) }
}

@Composable
fun rememberHaptics(): Haptics {
    val h = LocalHapticFeedback.current
    return Haptics(h::performHapticFeedback, LocalHapticsEnabled.current)
}

/** Shape that morphs between two Material shape polygons; progress 0 → start, 1 → end. */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(fit(android.graphics.Path(morph.toPath(progress)), size).asComposePath())
}

class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(fit(android.graphics.Path(polygon.toPath()), size).asComposePath())
}

private fun fit(path: android.graphics.Path, size: Size): android.graphics.Path {
    val bounds = RectF()
    path.computeBounds(bounds, true)
    val m = android.graphics.Matrix().apply {
        setScale(size.width / bounds.width(), size.height / bounds.height())
        postTranslate(-bounds.left * size.width / bounds.width(), -bounds.top * size.height / bounds.height())
    }
    path.transform(m)
    return path
}
