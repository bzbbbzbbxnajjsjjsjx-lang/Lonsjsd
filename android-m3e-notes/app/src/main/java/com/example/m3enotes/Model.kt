package com.example.m3enotes

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** A note. [updated] is epoch milliseconds. */
data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val updated: Long,
    val pinned: Boolean = false,
)

enum class SortOrder { UPDATED, TITLE }

enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Notes matching [query] in title or body, pinned notes first, then by [sort]. */
fun List<Note>.arrange(query: String, sort: SortOrder): List<Note> {
    val q = query.trim()
    val matching = if (q.isEmpty()) this else filter {
        it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true)
    }
    val pinnedFirst = compareByDescending<Note> { it.pinned }
    return when (sort) {
        SortOrder.UPDATED -> matching.sortedWith(pinnedFirst.thenByDescending { it.updated })
        SortOrder.TITLE -> matching.sortedWith(pinnedFirst.thenBy { it.title.lowercase() })
    }
}

/** Notes kept as one JSON file in the app's private storage. */
class NoteStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")

    fun load(): List<Note> = try {
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Note(o.getLong("id"), o.getString("title"), o.getString("body"), o.getLong("updated"), o.optBoolean("pinned"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("updated", n.updated)
                    .put("pinned", n.pinned),
            )
        }
        file.writeText(array.toString())
    }
}

/** Preferences from the Ayarlar screen. */
class Preferences(context: Context) {
    private val prefs = context.getSharedPreferences("m3e_notes", Context.MODE_PRIVATE)

    fun theme(): ThemeChoice = ThemeChoice.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeChoice.SYSTEM

    fun setTheme(value: ThemeChoice) = prefs.edit().putString(KEY_THEME, value.name).apply()

    fun sort(): SortOrder = SortOrder.entries.firstOrNull { it.name == prefs.getString(KEY_SORT, null) } ?: SortOrder.UPDATED

    fun setSort(value: SortOrder) = prefs.edit().putString(KEY_SORT, value.name).apply()

    fun pinnedOnTop(): Boolean = prefs.getBoolean(KEY_PIN_TOP, true)

    fun setPinnedOnTop(value: Boolean) = prefs.edit().putBoolean(KEY_PIN_TOP, value).apply()

    /** Wallpaper colours on Android 12 and newer. Falls back to the teal palette when off. */
    fun dynamicColor(): Boolean = prefs.getBoolean(KEY_DYNAMIC, true)

    fun setDynamicColor(value: Boolean) = prefs.edit().putBoolean(KEY_DYNAMIC, value).apply()

    /** False until the welcome screen has been dismissed once. */
    fun welcomed(): Boolean = prefs.getBoolean(KEY_WELCOMED, false)

    fun setWelcomed() = prefs.edit().putBoolean(KEY_WELCOMED, true).apply()

    /** Restores the defaults of every setting, without touching the notes. */
    fun resetToDefaults() = prefs.edit()
        .putString(KEY_THEME, ThemeChoice.SYSTEM.name)
        .putString(KEY_SORT, SortOrder.UPDATED.name)
        .putBoolean(KEY_PIN_TOP, true)
        .putBoolean(KEY_DYNAMIC, true)
        .apply()

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SORT = "sort"
        const val KEY_PIN_TOP = "pin_top"
        const val KEY_DYNAMIC = "dynamic"
        const val KEY_WELCOMED = "welcomed"
    }
}
