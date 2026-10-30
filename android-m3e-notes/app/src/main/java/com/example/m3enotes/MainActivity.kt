package com.example.m3enotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesApp(NoteStore(this), Preferences(this))
        }
    }
}

/** The three screens of the M3E Canvas design: Liste, Not, Ayarlar. */
private sealed interface Screen {
    data object List : Screen
    data class Note(val id: Long) : Screen
    data object Prefs : Screen
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesApp(store: NoteStore, prefs: Preferences) {
    var notes by remember { mutableStateOf(store.load()) }
    var theme by remember { mutableStateOf(prefs.theme()) }
    var sort by remember { mutableStateOf(prefs.sort()) }
    var pinTop by remember { mutableStateOf(prefs.pinnedOnTop()) }
    var screen by remember { mutableStateOf<Screen>(Screen.List) }
    // Leaving a note by its save button slides down; by back it fades, as the design specifies.
    var slideDownOnReturn by remember { mutableStateOf(false) }

    fun save(updated: List<com.example.m3enotes.Note>) {
        notes = updated
        store.save(updated)
    }

    fun commit(id: Long, title: String, body: String) {
        val blank = title.isBlank() && body.isBlank()
        save(
            if (blank) notes.filterNot { it.id == id }
            else notes.map { if (it.id == id) it.copy(title = title, body = body, updated = System.currentTimeMillis()) else it },
        )
        screen = Screen.List
    }

    BackHandler(enabled = screen == Screen.Prefs) { screen = Screen.List }

    val spatialInt = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val spatialFloat = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    NotesTheme(theme) {
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    // list -> note: expand from the card (design: "expand")
                    targetState is Screen.Note ->
                        (fadeIn(effects) + scaleIn(spatialFloat, initialScale = 0.85f)).togetherWith(fadeOut(effects))
                    // note -> list by save: slide down (design: "slideDown"); by back: fade (design: "fade")
                    initialState is Screen.Note && targetState == Screen.List && slideDownOnReturn ->
                        (fadeIn(effects) + slideInVertically(spatialInt) { -it / 8 })
                            .togetherWith(fadeOut(effects) + slideOutVertically(spatialInt) { it / 6 })
                    initialState is Screen.Note && targetState == Screen.List ->
                        fadeIn(effects).togetherWith(fadeOut(effects) + scaleOut(spatialFloat, targetScale = 0.85f))
                    // list -> prefs: slide left (design: "slideLeft"); back reverses it
                    targetState == Screen.Prefs ->
                        (slideInHorizontally(spatialInt) { it } + fadeIn(effects))
                            .togetherWith(slideOutHorizontally(spatialInt) { -it / 3 } + fadeOut(effects))
                    else ->
                        (slideInHorizontally(spatialInt) { -it / 3 } + fadeIn(effects))
                            .togetherWith(slideOutHorizontally(spatialInt) { it } + fadeOut(effects))
                }
            },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.List -> ListScreen(
                    notes = notes,
                    sort = sort,
                    pinTop = pinTop,
                    onOpen = { screen = Screen.Note(it) },
                    onNew = {
                        val id = System.currentTimeMillis()
                        save(listOf(com.example.m3enotes.Note(id, "", "", id)) + notes)
                        screen = Screen.Note(id)
                    },
                    onPrefs = { screen = Screen.Prefs },
                )
                is Screen.Note -> {
                    val note = notes.firstOrNull { it.id == current.id }
                    if (note == null) {
                        screen = Screen.List
                    } else {
                        NoteScreen(
                            note = note,
                            onBack = { title, body ->
                                slideDownOnReturn = false
                                commit(note.id, title, body)
                            },
                            onSave = { title, body ->
                                slideDownOnReturn = true
                                commit(note.id, title, body)
                            },
                            onTogglePin = {
                                save(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it })
                            },
                            onDelete = {
                                slideDownOnReturn = false
                                save(notes.filterNot { it.id == note.id })
                                screen = Screen.List
                            },
                        )
                    }
                }
                Screen.Prefs -> PrefsScreen(
                    theme = theme,
                    sort = sort,
                    pinTop = pinTop,
                    onTheme = {
                        theme = it
                        prefs.setTheme(it)
                    },
                    onSort = {
                        sort = it
                        prefs.setSort(it)
                    },
                    onPinTop = {
                        pinTop = it
                        prefs.setPinnedOnTop(it)
                    },
                    onDeleteAll = { save(emptyList()) },
                    onBack = { screen = Screen.List },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListScreen(
    notes: List<com.example.m3enotes.Note>,
    sort: SortOrder,
    pinTop: Boolean,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onPrefs: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    // "Yeni not" shrinks to an icon once the list scrolls (design: collapseOnScroll).
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val shown = remember(notes, query, sort, pinTop) {
        val arranged = notes.arrange(query, sort)
        if (pinTop) arranged else arranged.sortedBy { it.id }.reversed()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            // topAppBar: title with a preferences action on the right.
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.list_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPrefs) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.prefs_title))
                }
            }

            Spacer(Modifier.height(12.dp))

            // searchBar (design: "Ara", search icon, filled)
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(12.dp))

            if (shown.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(if (query.isBlank()) R.string.no_notes else R.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // listItem run (design: axis y), segmented corners
                    itemsIndexed(shown, key = { _, note -> note.id }) { index, note ->
                        NoteRow(note = note, shape = segmentShape(index, shown.size), onClick = { onOpen(note.id) })
                    }
                }
            }
        }

        // extendedFab (design: "Yeni not", collapseOnScroll)
        ExtendedFloatingActionButton(
            onClick = onNew,
            expanded = fabExpanded,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(20.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_note)) },
        )
    }
}

@Composable
private fun NoteRow(note: com.example.m3enotes.Note, shape: Shape, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.body.isNotBlank()) {
                    Text(
                        text = note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatDate(note.updated),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (note.pinned) {
                Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.unpin), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoteScreen(
    note: com.example.m3enotes.Note,
    onBack: (String, String) -> Unit,
    onSave: (String, String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    // The draft lives here and is committed only on save or back.
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    BackHandler { onBack(title, body) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            // topAppBar: back (fade) on the left, delete on the right
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBack(title, body) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.note_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                }
            }

            // textField (outlined) for the title and the body
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.title_hint)) },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text(stringResource(R.string.body_hint)) },
            )
            Spacer(Modifier.height(88.dp))
        }

        // toolbar (design: tonal, pin and delete)
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(start = 20.dp, bottom = 20.dp),
        ) {
            IconButton(onClick = onTogglePin) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(if (note.pinned) R.string.unpin else R.string.pin),
                    tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }

        // fab (design: check, saves and slides back to the list)
        FloatingActionButton(
            onClick = { onSave(title, body) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrefsScreen(
    theme: ThemeChoice,
    sort: SortOrder,
    pinTop: Boolean,
    onTheme: (ThemeChoice) -> Unit,
    onSort: (SortOrder) -> Unit,
    onPinTop: (Boolean) -> Unit,
    onDeleteAll: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                text = stringResource(R.string.prefs_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(12.dp))

        // select (design: Tema)
        ChoiceSelect(
            label = stringResource(R.string.pref_theme),
            current = stringResource(
                when (theme) {
                    ThemeChoice.SYSTEM -> R.string.theme_system
                    ThemeChoice.LIGHT -> R.string.theme_light
                    ThemeChoice.DARK -> R.string.theme_dark
                },
            ),
            options = ThemeChoice.entries.map { option ->
                stringResource(
                    when (option) {
                        ThemeChoice.SYSTEM -> R.string.theme_system
                        ThemeChoice.LIGHT -> R.string.theme_light
                        ThemeChoice.DARK -> R.string.theme_dark
                    },
                ) to { onTheme(option) }
            },
        )
        Spacer(Modifier.height(12.dp))

        // select (design: Sıralama)
        ChoiceSelect(
            label = stringResource(R.string.pref_sort),
            current = stringResource(if (sort == SortOrder.UPDATED) R.string.sort_updated else R.string.sort_title),
            options = SortOrder.entries.map { option ->
                stringResource(if (option == SortOrder.UPDATED) R.string.sort_updated else R.string.sort_title) to { onSort(option) }
            },
        )
        Spacer(Modifier.height(12.dp))

        // switch row (design: Sabitlileri üstte tut)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.pref_pin_top),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Switch(checked = pinTop, onCheckedChange = onPinTop)
        }
        Spacer(Modifier.height(20.dp))

        // button (design: tonal "Tüm notları sil", asks first)
        Button(
            onClick = { confirmDelete = true },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.pref_delete_all))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAll()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** A labelled select: an outlined button that opens a menu of options. */
@Composable
private fun ChoiceSelect(label: String, current: String, options: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Text(current, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (text, pick) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            open = false
                            pick()
                        },
                    )
                }
            }
        }
    }
}

/** Outer corners round on the first and last row, inner corners tight: one grouped card. */
private fun segmentShape(index: Int, count: Int): Shape {
    val outer = 28.dp
    val inner = 4.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.getDefault()))
