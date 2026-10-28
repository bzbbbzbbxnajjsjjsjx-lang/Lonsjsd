package com.example.minilauncher

import android.content.Context

/** User preferences for the launcher. Stored locally on the device. */
data class LauncherSettings(
    val autoKeyboard: Boolean = true,
    val autoOpen: Boolean = true,
    val sortByUsage: Boolean = true,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): LauncherSettings = LauncherSettings(
        autoKeyboard = prefs.getBoolean(KEY_AUTO_KEYBOARD, true),
        autoOpen = prefs.getBoolean(KEY_AUTO_OPEN, true),
        sortByUsage = prefs.getBoolean(KEY_SORT_USAGE, true),
    )

    fun save(settings: LauncherSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_KEYBOARD, settings.autoKeyboard)
            .putBoolean(KEY_AUTO_OPEN, settings.autoOpen)
            .putBoolean(KEY_SORT_USAGE, settings.sortByUsage)
            .apply()
    }

    private companion object {
        const val KEY_AUTO_KEYBOARD = "auto_keyboard"
        const val KEY_AUTO_OPEN = "auto_open"
        const val KEY_SORT_USAGE = "sort_usage"
    }
}
