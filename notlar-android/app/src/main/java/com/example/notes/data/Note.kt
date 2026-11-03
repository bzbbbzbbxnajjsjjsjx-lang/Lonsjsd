package com.example.notes.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One line of a checklist note. */
data class CheckItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val checked: Boolean = false,
)

/**
 * A note. Text notes use [body]; checklist notes use [items]. Switching between the two
 * converts the content, so nothing typed is lost.
 */
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val isChecklist: Boolean = false,
    val items: List<CheckItem> = emptyList(),
    /** Index into [NoteColors]; 0 is the default surface. */
    val color: Int = 0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    /** When the note was moved to the trash, or null when it is not in the trash. */
    val trashedAt: Long? = null,
    val labels: List<String> = emptyList(),
    /** Reminder time in epoch millis, or null. */
    val reminder: Long? = null,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
) {
    val isBlank: Boolean
        get() = title.isBlank() && body.isBlank() && items.all { it.text.isBlank() }

    val inTrash: Boolean get() = trashedAt != null

    /** Plain text of the note, used for search, sharing and word counts. */
    fun plainText(): String = buildString {
        if (title.isNotBlank()) appendLine(title)
        if (isChecklist) {
            items.forEach { appendLine((if (it.checked) "☑ " else "☐ ") + it.text) }
        } else {
            append(body)
        }
    }.trim()

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim()
        return title.contains(q, ignoreCase = true) ||
            body.contains(q, ignoreCase = true) ||
            items.any { it.text.contains(q, ignoreCase = true) } ||
            labels.any { it.contains(q, ignoreCase = true) }
    }

    /** Converts a text note into a checklist (one item per line) and back. */
    fun toggledChecklist(): Note = if (isChecklist) {
        copy(isChecklist = false, body = items.joinToString("\n") { it.text }, items = emptyList())
    } else {
        val lines = body.lines().filter { it.isNotBlank() }
        copy(
            isChecklist = true,
            items = if (lines.isEmpty()) listOf(CheckItem()) else lines.map { CheckItem(text = it) },
            body = "",
        )
    }
}

/** JSON encoding of notes, kept free of Android types so it can be unit tested. */
object NoteJson {
    const val VERSION = 2

    fun encode(notes: List<Note>): String = JSONObject()
        .put("version", VERSION)
        .put("notes", JSONArray().apply { notes.forEach { put(encodeNote(it)) } })
        .toString()

    /** Reads both this format and the first version's bare array of {id,title,body,updated}. */
    fun decode(json: String): List<Note> {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()
        val array = if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed).optJSONArray("notes") ?: JSONArray()
        return (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(::decodeNote) }
    }

    private fun encodeNote(n: Note) = JSONObject().apply {
        put("id", n.id)
        put("title", n.title)
        put("body", n.body)
        put("checklist", n.isChecklist)
        put("items", JSONArray().apply {
            n.items.forEach { put(JSONObject().put("id", it.id).put("text", it.text).put("checked", it.checked)) }
        })
        put("color", n.color)
        put("pinned", n.pinned)
        put("archived", n.archived)
        n.trashedAt?.let { put("trashedAt", it) }
        put("labels", JSONArray(n.labels))
        n.reminder?.let { put("reminder", it) }
        put("created", n.created)
        put("updated", n.updated)
    }

    private fun decodeNote(o: JSONObject): Note? {
        val id = o.optString("id").ifEmpty { return null }
        val updated = o.optLong("updated", System.currentTimeMillis())
        val items = o.optJSONArray("items")?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                a.optJSONObject(i)?.let {
                    CheckItem(
                        id = it.optString("id").ifEmpty { UUID.randomUUID().toString() },
                        text = it.optString("text"),
                        checked = it.optBoolean("checked"),
                    )
                }
            }
        } ?: emptyList()
        val labels = o.optJSONArray("labels")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } } ?: emptyList()
        return Note(
            id = id,
            title = o.optString("title"),
            body = o.optString("body"),
            isChecklist = o.optBoolean("checklist"),
            items = items,
            color = o.optInt("color", 0).coerceIn(0, NoteColors.size - 1),
            pinned = o.optBoolean("pinned"),
            archived = o.optBoolean("archived"),
            trashedAt = if (o.has("trashedAt")) o.optLong("trashedAt") else null,
            labels = labels,
            reminder = if (o.has("reminder")) o.optLong("reminder") else null,
            created = o.optLong("created", updated),
            updated = updated,
        )
    }
}

/** Number of note colors, including the default (index 0). The palette lives in the UI theme. */
object NoteColors {
    const val size = 12
}
