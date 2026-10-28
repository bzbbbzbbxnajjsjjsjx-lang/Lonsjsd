package com.example.minilauncher

import org.json.JSONArray
import org.json.JSONObject

/** Everything the user configured: settings, favourites, hidden apps and usage counts. */
data class LauncherBackup(
    val settings: LauncherSettings,
    val favorites: List<String>,
    val hidden: Set<String>,
    val usage: Map<String, Int>,
)

object BackupCodec {

    fun encode(backup: LauncherBackup): String {
        val root = JSONObject()
            .put("version", 1)
            .put("autoKeyboard", backup.settings.autoKeyboard)
            .put("autoOpen", backup.settings.autoOpen)
            .put("sortByUsage", backup.settings.sortByUsage)
            .put("showHidden", backup.settings.showHidden)
            .put("themeMode", backup.settings.themeMode.name)
            .put("accent", backup.settings.accent.name)
            .put("favorites", JSONArray(backup.favorites))
            .put("hidden", JSONArray(backup.hidden.toList()))
        val usage = JSONObject()
        backup.usage.forEach { (key, count) -> usage.put(key, count) }
        root.put("usage", usage)
        return root.toString(2)
    }

    /** Throws if the text is not a backup written by [encode]. */
    fun decode(text: String): LauncherBackup {
        val root = JSONObject(text)
        val settings = LauncherSettings(
            autoKeyboard = root.getBoolean("autoKeyboard"),
            autoOpen = root.getBoolean("autoOpen"),
            sortByUsage = root.getBoolean("sortByUsage"),
            showHidden = root.getBoolean("showHidden"),
            themeMode = ThemeMode.entries.first { it.name == root.getString("themeMode") },
            accent = AccentColor.entries.first { it.name == root.getString("accent") },
        )
        val favorites = root.getJSONArray("favorites").let { array -> List(array.length()) { array.getString(it) } }
        val hidden = root.getJSONArray("hidden").let { array -> List(array.length()) { array.getString(it) } }.toSet()
        val usageObject = root.getJSONObject("usage")
        val usage = usageObject.keys().asSequence().associateWith { usageObject.getInt(it) }
        return LauncherBackup(settings, favorites, hidden, usage)
    }
}
