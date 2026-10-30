package com.example.m3enotes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The teal palette from the M3E Canvas design (paletteKey "teal"). */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EF2F0),
    onPrimaryContainer = Color(0xFF002020),
    secondaryContainer = Color(0xFFCBE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiaryContainer = Color(0xFFFFDAD6),
    onTertiaryContainer = Color(0xFF410002),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF80D5D4),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF9EF2F0),
    secondaryContainer = Color(0xFF334B4B),
    onSecondaryContainer = Color(0xFFCBE8E7),
    tertiaryContainer = Color(0xFF93000A),
    onTertiaryContainer = Color(0xFFFFDAD6),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesTheme(choice: ThemeChoice, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    MaterialExpressiveTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
