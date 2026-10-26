package com.example.notes

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Note(val id: String, val title: String, val body: String, val updated: Long)

/** Keeps all notes as one JSON list in SharedPreferences. */
class NoteStore(private val prefs: SharedPreferences) {

    fun load(): List<Note> = runCatching {
        val array = JSONArray(prefs.getString("notes", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Note(o.getString("id"), o.getString("title"), o.getString("body"), o.getLong("updated"))
        }
    }.getOrDefault(emptyList())

    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("updated", n.updated),
            )
        }
        prefs.edit().putString("notes", array.toString()).apply()
    }
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val store = NoteStore(application.getSharedPreferences("notes", Context.MODE_PRIVATE))

    private val _notes = MutableStateFlow(store.load().sortedByDescending { it.updated })

    /** All notes, most recently changed first. */
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    /** A new id for a note that has not been written yet. */
    fun newId(): String = UUID.randomUUID().toString()

    /** Writes the note as it is typed. A note with neither a title nor text is removed. */
    fun save(id: String, title: String, body: String) {
        if (title.isBlank() && body.isBlank()) {
            delete(id)
        } else {
            commit(_notes.value.filterNot { it.id == id } + Note(id, title, body, System.currentTimeMillis()))
        }
    }

    fun delete(id: String) = commit(_notes.value.filterNot { it.id == id })

    private fun commit(list: List<Note>) {
        val sorted = list.sortedByDescending { it.updated }
        store.save(sorted)
        _notes.value = sorted
    }
}
