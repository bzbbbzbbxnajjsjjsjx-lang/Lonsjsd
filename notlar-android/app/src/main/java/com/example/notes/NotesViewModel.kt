package com.example.notes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notes.data.CheckItem
import com.example.notes.data.Note
import com.example.notes.data.NoteJson
import com.example.notes.data.NoteStore
import com.example.notes.data.Settings
import com.example.notes.data.SettingsStore
import com.example.notes.data.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Which notes the list shows. */
sealed interface Filter {
    data object Notes : Filter
    data object Reminders : Filter
    data object Archive : Filter
    data object Trash : Filter
    data class Label(val name: String) : Filter
}

data class UiState(
    val notes: List<Note> = emptyList(),
    val settings: Settings = Settings(),
    val filter: Filter = Filter.Notes,
    val query: String = "",
    val editingId: String? = null,
    val selection: Set<String> = emptySet(),
    val showSettings: Boolean = false,
    val loaded: Boolean = false,
) {
    val editing: Note? get() = editingId?.let { id -> notes.firstOrNull { it.id == id } }

    /** Notes for the current filter and search, pinned first, then by the chosen order. */
    val visible: List<Note>
        get() = notes.asSequence()
            .filter {
                when (filter) {
                    Filter.Notes -> !it.archived && !it.inTrash
                    Filter.Reminders -> it.reminder != null && !it.inTrash
                    Filter.Archive -> it.archived && !it.inTrash
                    Filter.Trash -> it.inTrash
                    is Filter.Label -> filter.name in it.labels && !it.inTrash
                }
            }
            .filter { it.matches(query) }
            .sortedWith(comparator(settings.sort, pinnedFirst = filter != Filter.Trash))
            .toList()

    val allLabels: List<String>
        get() = (settings.labels + notes.flatMap { it.labels }).distinct().sortedBy { it.lowercase() }
}

fun comparator(sort: SortOrder, pinnedFirst: Boolean): Comparator<Note> {
    val base: Comparator<Note> = when (sort) {
        SortOrder.Updated -> compareByDescending { it.updated }
        SortOrder.Created -> compareByDescending { it.created }
        SortOrder.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "￿" } }
    }
    return if (pinnedFirst) compareByDescending<Note> { it.pinned }.then(base) else base
}

/** A message for the snackbar, optionally with an undo action. */
data class Snack(val text: String, val undo: (() -> Unit)? = null)

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val store = NoteStore(app)
    private val settingsStore = SettingsStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val snacks = Channel<Snack>(Channel.BUFFERED)
    val snackbar = snacks.receiveAsFlow()

    // Editor history for undo and redo. Typing within a short window collapses into one step.
    private val undoStack = ArrayDeque<Note>()
    private val redoStack = ArrayDeque<Note>()
    private var lastHistoryPush = 0L
    private val _history = MutableStateFlow(false to false)
    /** (canUndo, canRedo) for the open note. */
    val history: StateFlow<Pair<Boolean, Boolean>> = _history.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val week = TimeUnit.DAYS.toMillis(7)
            val now = System.currentTimeMillis()
            // Notes in the trash for more than a week are gone for good.
            val notes = store.load().filter { it.trashedAt == null || now - it.trashedAt < week }
            val settings = settingsStore.load()
            // Keep anything created before loading finished (a shortcut or shared text).
            _state.update { s ->
                val early = s.notes.map { it.id }.toSet()
                s.copy(notes = s.notes + notes.filterNot { it.id in early }, settings = settings, loaded = true)
            }
        }
    }

    // region Navigation

    fun setFilter(filter: Filter) = _state.update { it.copy(filter = filter, selection = emptySet(), showSettings = false) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun openSettings(show: Boolean) = _state.update { it.copy(showSettings = show) }

    /** Notes created in this session that have not been typed in yet; they vanish if left empty. */
    private val freshIds = mutableSetOf<String>()

    fun open(id: String) {
        if (state.value.editingId != id) dropIfBlank()
        resetHistory()
        _state.update { it.copy(editingId = id, selection = emptySet()) }
    }

    /** Creates an empty note, opens it and returns its id. */
    fun create(checklist: Boolean = false, body: String = "", title: String = ""): String {
        val label = (state.value.filter as? Filter.Label)?.name
        val note = Note(
            title = title,
            body = if (checklist) "" else body,
            isChecklist = checklist,
            items = if (checklist) listOf(CheckItem()) else emptyList(),
            labels = listOfNotNull(label),
        )
        if (state.value.editingId != null) dropIfBlank()
        _state.update { it.copy(notes = listOf(note) + it.notes) }
        freshIds += note.id
        open(note.id)
        return note.id
    }

    /** Closes the editor. */
    fun close() {
        dropIfBlank()
        _state.update { it.copy(editingId = null) }
        resetHistory()
    }

    /**
     * An empty note that was just created is dropped without a trace. An existing note the
     * user emptied goes to the trash with undo, so nothing is lost by accident.
     */
    private fun dropIfBlank() {
        val note = state.value.editing ?: return
        if (!note.isBlank) return
        if (note.id in freshIds) {
            _state.update { s -> s.copy(notes = s.notes.filterNot { it.id == note.id }) }
            Reminders.cancel(getApplication(), note.id)
            persist()
        } else {
            trash(listOf(note.id))
        }
        freshIds -= note.id
    }

    // endregion

    // region Editing

    /** Applies an edit to the open note and records it for undo. */
    fun edit(transform: (Note) -> Note) {
        val current = state.value.editing ?: return
        val next = transform(current).copy(updated = System.currentTimeMillis())
        val now = System.currentTimeMillis()
        if (now - lastHistoryPush > 700 || undoStack.isEmpty()) {
            undoStack.addLast(current)
            if (undoStack.size > 100) undoStack.removeFirst()
        }
        lastHistoryPush = now
        redoStack.clear()
        replace(next)
        if (!next.isBlank) freshIds -= next.id
        publishHistory()
    }

    fun undo() {
        val current = state.value.editing ?: return
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current)
        lastHistoryPush = 0
        replace(current.withContentOf(prev))
        publishHistory()
    }

    fun redo() {
        val current = state.value.editing ?: return
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current)
        lastHistoryPush = 0
        replace(current.withContentOf(next))
        publishHistory()
    }

    /** Undo covers what is typed and styled, never pin, archive or reminder state. */
    private fun Note.withContentOf(other: Note) = copy(
        title = other.title, body = other.body, isChecklist = other.isChecklist, items = other.items,
        color = other.color, labels = other.labels, updated = System.currentTimeMillis(),
    )

    private fun resetHistory() {
        undoStack.clear(); redoStack.clear(); lastHistoryPush = 0; publishHistory()
    }

    private fun publishHistory() { _history.value = undoStack.isNotEmpty() to redoStack.isNotEmpty() }

    private fun replace(note: Note) {
        _state.update { s -> s.copy(notes = s.notes.map { if (it.id == note.id) note else it }) }
        persist()
    }

    // endregion

    // region Note actions (work on one id or on the selection)

    private fun mutate(ids: Collection<String>, touch: Boolean = true, f: (Note) -> Note) {
        val now = System.currentTimeMillis()
        _state.update { s ->
            s.copy(notes = s.notes.map { if (it.id in ids) f(it).let { n -> if (touch) n.copy(updated = now) else n } else it })
        }
        persist()
    }

    fun togglePin(ids: Collection<String>) {
        val notes = state.value.notes.filter { it.id in ids }
        val pin = notes.any { !it.pinned }
        mutate(ids, touch = false) { it.copy(pinned = pin, archived = if (pin) false else it.archived) }
        clearSelection()
    }

    fun archive(ids: Collection<String>, archive: Boolean = true) {
        val before = state.value.notes.filter { it.id in ids }
        mutate(ids, touch = false) { it.copy(archived = archive, pinned = if (archive) false else it.pinned) }
        if (state.value.editingId in ids) _state.update { it.copy(editingId = null) }
        clearSelection()
        snacks.trySend(Snack(if (archive) countText(ids.size, "arşivlendi") else countText(ids.size, "arşivden çıkarıldı")) { restore(before) })
    }

    fun trash(ids: Collection<String>) {
        val before = state.value.notes.filter { it.id in ids }
        val now = System.currentTimeMillis()
        mutate(ids, touch = false) { it.copy(trashedAt = now, pinned = false) }
        ids.forEach { Reminders.cancel(getApplication(), it) }
        if (state.value.editingId in ids) _state.update { it.copy(editingId = null) }
        clearSelection()
        snacks.trySend(Snack(countText(ids.size, "çöp kutusuna taşındı")) { restore(before) })
    }

    fun restoreFromTrash(ids: Collection<String>) {
        mutate(ids, touch = false) { it.copy(trashedAt = null) }
        state.value.notes.filter { it.id in ids }.forEach { Reminders.schedule(getApplication(), it) }
        clearSelection()
        snacks.trySend(Snack(countText(ids.size, "geri yüklendi")))
    }

    fun deleteForever(ids: Collection<String>) {
        val before = state.value.notes.filter { it.id in ids }
        _state.update { s -> s.copy(notes = s.notes.filterNot { it.id in ids }, editingId = s.editingId.takeUnless { it in ids }) }
        persist()
        clearSelection()
        snacks.trySend(Snack(countText(ids.size, "kalıcı olarak silindi")) { restore(before, add = true) })
    }

    fun emptyTrash() = deleteForever(state.value.notes.filter { it.inTrash }.map { it.id })

    fun setColor(ids: Collection<String>, color: Int) = mutate(ids) { it.copy(color = color) }

    fun duplicate(id: String) {
        val src = state.value.notes.firstOrNull { it.id == id } ?: return
        val now = System.currentTimeMillis()
        val copy = src.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = if (src.title.isBlank()) "" else src.title + " (kopya)",
            items = src.items.map { it.copy(id = java.util.UUID.randomUUID().toString()) },
            pinned = false, reminder = null, created = now, updated = now,
        )
        _state.update { s -> s.copy(notes = listOf(copy) + s.notes, selection = emptySet()) }
        persist()
        snacks.trySend(Snack("Not kopyalandı"))
    }

    fun setReminder(id: String, time: Long?) {
        if (time != null && time <= System.currentTimeMillis()) {
            snacks.trySend(Snack("Geçmiş bir zaman seçilemez"))
            return
        }
        mutate(listOf(id), touch = false) { it.copy(reminder = time) }
        val note = state.value.notes.firstOrNull { it.id == id } ?: return
        if (time == null) Reminders.cancel(getApplication(), id) else Reminders.schedule(getApplication(), note)
    }

    fun toggleLabel(ids: Collection<String>, label: String) {
        val all = state.value.notes.filter { it.id in ids }.all { label in it.labels }
        mutate(ids, touch = false) { n -> n.copy(labels = if (all) n.labels - label else (n.labels + label).distinct()) }
    }

    /** Puts notes back the way they were, used by undo. */
    private fun restore(notes: List<Note>, add: Boolean = false) {
        val byId = notes.associateBy { it.id }
        _state.update { s ->
            if (add) s.copy(notes = notes + s.notes.filterNot { it.id in byId })
            else s.copy(notes = s.notes.map { byId[it.id] ?: it })
        }
        notes.forEach { Reminders.schedule(getApplication(), it) }
        persist()
    }

    private fun countText(n: Int, verb: String) = if (n == 1) "Not $verb" else "$n not $verb"

    // endregion

    // region Selection

    fun toggleSelect(id: String) = _state.update {
        it.copy(selection = if (id in it.selection) it.selection - id else it.selection + id)
    }

    fun selectAll() = _state.update { s -> s.copy(selection = s.visible.map { it.id }.toSet()) }
    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    // endregion

    // region Labels

    fun addLabel(name: String) {
        val clean = name.trim().take(40)
        if (clean.isEmpty()) return
        updateSettings { it.copy(labels = (it.labels + clean).distinct()) }
    }

    fun renameLabel(old: String, new: String) {
        val clean = new.trim().take(40)
        if (clean.isEmpty() || clean == old) return
        updateSettings { s -> s.copy(labels = s.labels.map { if (it == old) clean else it }.distinct()) }
        _state.update { s ->
            s.copy(
                notes = s.notes.map { n -> if (old in n.labels) n.copy(labels = n.labels.map { if (it == old) clean else it }.distinct()) else n },
                filter = if (s.filter == Filter.Label(old)) Filter.Label(clean) else s.filter,
            )
        }
        persist()
    }

    fun deleteLabel(name: String) {
        updateSettings { s -> s.copy(labels = s.labels - name) }
        _state.update { s ->
            s.copy(
                notes = s.notes.map { n -> if (name in n.labels) n.copy(labels = n.labels - name) else n },
                filter = if (s.filter == Filter.Label(name)) Filter.Notes else s.filter,
            )
        }
        persist()
    }

    // endregion

    // region Settings, import, export

    fun updateSettings(f: (Settings) -> Settings) {
        _state.update { it.copy(settings = f(it.settings)) }
        val s = state.value.settings
        viewModelScope.launch(Dispatchers.IO) { settingsStore.save(s) }
    }

    fun export(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val ok = runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use {
                it.write(NoteJson.encode(state.value.notes).toByteArray())
            }
        }.isSuccess
        snacks.trySend(Snack(if (ok) "${state.value.notes.size} not dışa aktarıldı" else "Dışa aktarılamadı"))
    }

    fun import(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val imported = runCatching {
            getApplication<Application>().contentResolver.openInputStream(uri)!!.use { NoteJson.decode(it.readBytes().decodeToString()) }
        }.getOrNull()
        if (imported == null) {
            snacks.trySend(Snack("Dosya okunamadı"))
            return@launch
        }
        val existing = state.value.notes.map { it.id }.toSet()
        val fresh = imported.filterNot { it.id in existing }
        _state.update { s -> s.copy(notes = fresh + s.notes) }
        persist()
        fresh.forEach { Reminders.schedule(getApplication(), it) }
        snacks.trySend(Snack("${fresh.size} not içe aktarıldı"))
    }

    // endregion

    // One writer at a time, always writing the newest list, so saves never land out of order.
    private val writer = Dispatchers.IO.limitedParallelism(1)

    private var pendingSave: kotlinx.coroutines.Job? = null

    /** Saves shortly after the last change, so a burst of typing is one write. */
    private fun persist() {
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch(writer) {
            kotlinx.coroutines.delay(300)
            write()
        }
    }

    /** Writes now; called when the app goes to the background. */
    fun flush() {
        if (pendingSave?.isActive != true) return
        pendingSave?.cancel()
        kotlinx.coroutines.CoroutineScope(writer).launch { write() }
    }

    private fun write() {
        if (!state.value.loaded) return
        runCatching { store.save(state.value.notes) }
            .onFailure { snacks.trySend(Snack("Notlar kaydedilemedi")) }
    }

    override fun onCleared() {
        if (pendingSave?.isActive == true) runCatching { store.save(state.value.notes) }
    }
}
