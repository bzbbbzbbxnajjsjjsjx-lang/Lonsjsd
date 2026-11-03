package com.example.notes.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }
enum class SortOrder { Updated, Created, Title }

data class Settings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    /** Pure black surfaces in dark theme. */
    val amoled: Boolean = false,
    val grid: Boolean = true,
    val sort: SortOrder = SortOrder.Updated,
    val haptics: Boolean = true,
    /** Body text size multiplier in the editor. */
    val textScale: Float = 1f,
    val showPreviewLines: Int = 6,
    val labels: List<String> = emptyList(),
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load() = Settings(
        themeMode = enumOr(prefs.getString("theme", null), ThemeMode.System),
        dynamicColor = prefs.getBoolean("dynamic", true),
        amoled = prefs.getBoolean("amoled", false),
        grid = prefs.getBoolean("grid", true),
        sort = enumOr(prefs.getString("sort", null), SortOrder.Updated),
        haptics = prefs.getBoolean("haptics", true),
        textScale = prefs.getFloat("textScale", 1f),
        showPreviewLines = prefs.getInt("previewLines", 6),
        labels = prefs.getString("labels", null)?.split('\u001F')?.filter { it.isNotBlank() } ?: emptyList(),
    )

    fun save(s: Settings) {
        prefs.edit()
            .putString("theme", s.themeMode.name)
            .putBoolean("dynamic", s.dynamicColor)
            .putBoolean("amoled", s.amoled)
            .putBoolean("grid", s.grid)
            .putString("sort", s.sort.name)
            .putBoolean("haptics", s.haptics)
            .putFloat("textScale", s.textScale)
            .putInt("previewLines", s.showPreviewLines)
            .putString("labels", s.labels.joinToString("\u001F"))
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
