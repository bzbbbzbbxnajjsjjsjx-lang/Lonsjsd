package com.example.notes.data

import android.content.Context
import java.io.File

/** Notes live in one JSON file, written atomically through a temp file. */
class NoteStore(private val context: Context) {
    private val file = File(context.filesDir, "notes.json")

    fun load(): List<Note> {
        if (!file.exists()) return runCatching { migrateFromPrefs() }.getOrDefault(emptyList())
        return try {
            NoteJson.decode(file.readText())
        } catch (_: Exception) {
            // Keep an unreadable file aside instead of letting the next save overwrite it.
            file.renameTo(File(file.parentFile, "notes.corrupt-${System.currentTimeMillis()}.json"))
            emptyList()
        }
    }

    @Synchronized
    fun save(notes: List<Note>) {
        val tmp = File(file.parentFile, "notes.json.tmp")
        tmp.writeText(NoteJson.encode(notes))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    /** The first version kept a JSON array in SharedPreferences "notes" under key "notes". */
    private fun migrateFromPrefs(): List<Note> {
        val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)
        val old = prefs.getString("notes", null) ?: return emptyList()
        val notes = NoteJson.decode(old)
        save(notes)
        prefs.edit().remove("notes").apply()
        return notes
    }
}
