package com.example.minilauncher

import android.content.Context

/** User preferences for the launcher. Stored locally on the device. */
data class LauncherSettings(
    val autoKeyboard: Boolean = true,
    val autoOpen: Boolean = true,
    val sortByUsage: Boolean = true,
    val showHidden: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.DYNAMIC,
    val haptics: Boolean = true,
    val showDock: Boolean = true,
    val compact: Boolean = false,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): LauncherSettings {
        // Older builds could save the switches as off. Turn them on once, on the first run of this version.
        if (prefs.getInt(KEY_VERSION, 0) < VERSION) {
            prefs.edit()
                .putBoolean(KEY_AUTO_KEYBOARD, true)
                .putBoolean(KEY_AUTO_OPEN, true)
                .putBoolean(KEY_SORT_USAGE, true)
                .putInt(KEY_VERSION, VERSION)
                .apply()
        }
        return LauncherSettings(
            autoKeyboard = prefs.getBoolean(KEY_AUTO_KEYBOARD, true),
            autoOpen = prefs.getBoolean(KEY_AUTO_OPEN, true),
            sortByUsage = prefs.getBoolean(KEY_SORT_USAGE, true),
            showHidden = prefs.getBoolean(KEY_SHOW_HIDDEN, false),
            themeMode = enumOrDefault(prefs.getString(KEY_THEME_MODE, null), ThemeMode.SYSTEM),
            accent = enumOrDefault(prefs.getString(KEY_ACCENT, null), AccentColor.DYNAMIC),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            showDock = prefs.getBoolean(KEY_SHOW_DOCK, true),
            compact = prefs.getBoolean(KEY_COMPACT, false),
        )
    }

    /** Reads a saved enum name, falling back to [default] for missing or unknown values. */
    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    fun save(settings: LauncherSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_KEYBOARD, settings.autoKeyboard)
            .putBoolean(KEY_AUTO_OPEN, settings.autoOpen)
            .putBoolean(KEY_SORT_USAGE, settings.sortByUsage)
            .putBoolean(KEY_SHOW_HIDDEN, settings.showHidden)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_ACCENT, settings.accent.name)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_SHOW_DOCK, settings.showDock)
            .putBoolean(KEY_COMPACT, settings.compact)
            .apply()
    }

    private companion object {
        const val VERSION = 2
        const val KEY_VERSION = "version"
        const val KEY_AUTO_KEYBOARD = "auto_keyboard"
        const val KEY_AUTO_OPEN = "auto_open"
        const val KEY_SORT_USAGE = "sort_usage"
        const val KEY_SHOW_HIDDEN = "show_hidden"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT = "accent"
        const val KEY_HAPTICS = "haptics"
        const val KEY_SHOW_DOCK = "show_dock"
        const val KEY_COMPACT = "compact"
    }
}

/** Apps the user hid from the list. Keys are ComponentName.flattenToString(). */
class HiddenStore(context: Context) {
    private val prefs = context.getSharedPreferences("hidden", Context.MODE_PRIVATE)

    fun load(): Set<String> = prefs.getStringSet(KEY, null)?.toSet() ?: emptySet()

    fun hide(key: String) {
        prefs.edit().putStringSet(KEY, load() + key).apply()
    }

    fun show(key: String) {
        prefs.edit().putStringSet(KEY, load() - key).apply()
    }

    fun replaceAll(keys: Set<String>) {
        prefs.edit().putStringSet(KEY, keys).apply()
    }

    private companion object {
        const val KEY = "keys"
    }
}
