package com.example.rss.ui

import android.graphics.RectF
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

/**
 * The app theme: Material 3 color roles (dynamic on Android 12+), and the Expressive
 * motion scheme, whose springs drive every animation the components run.
 */
@Composable
fun RssTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colors: ColorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

/**
 * A shape that morphs between two Material shape polygons. `progress` 0 draws [morph]'s
 * start shape, 1 its end shape; everything in between is interpolated along the outline.
 */
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = android.graphics.Path(morph.toPath(progress))
        val bounds = RectF()
        path.computeBounds(bounds, true)
        // Fill the whole box: the outline is stretched to the box's width and height.
        val matrix = android.graphics.Matrix().apply {
            setScale(size.width / bounds.width(), size.height / bounds.height())
            postTranslate(-bounds.left * size.width / bounds.width(), -bounds.top * size.height / bounds.height())
        }
        path.transform(matrix)
        return Outline.Generic(path.asComposePath())
    }
}
