package com.example.minnotes

import android.content.Context
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val updated: Long,
    val pinned: Boolean = false,
)

enum class SortOrder { UPDATED, TITLE }

/** Notes matching [query] (title or body, ignoring case), pinned first, then by the chosen order. */
fun List<Note>.visible(query: String, sort: SortOrder): List<Note> {
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
class NotesStore(context: Context) {
    private val file = File(context.filesDir, "notes.json")

    fun load(): List<Note> = try {
        val array = JSONArray(file.readText())
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Note(
                id = o.getLong("id"),
                title = o.getString("title"),
                body = o.getString("body"),
                updated = o.getLong("updated"),
                pinned = o.optBoolean("pinned", false),
            )
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

/** Display choices kept in shared preferences. */
class NotesPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)

    fun theme(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.SYSTEM

    fun setTheme(mode: ThemeMode) = prefs.edit().putString(KEY_THEME, mode.name).apply()

    fun sort(): SortOrder = SortOrder.entries.firstOrNull { it.name == prefs.getString(KEY_SORT, null) } ?: SortOrder.UPDATED

    fun setSort(order: SortOrder) = prefs.edit().putString(KEY_SORT, order.name).apply()

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_SORT = "sort"
    }
}
