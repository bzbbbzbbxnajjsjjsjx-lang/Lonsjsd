package com.example.minnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val store by lazy { NotesStore(this) }
    private val prefs by lazy { NotesPrefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NotesApp(store = store, prefs = prefs)
        }
    }
}

/** The three screens. Home is the list; Editor opens one note; Settings holds the preferences. */
private sealed interface Screen {
    data object Home : Screen
    data class Editor(val id: Long) : Screen
    data object Settings : Screen
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesApp(store: NotesStore, prefs: NotesPrefs) {
    var notes by remember { mutableStateOf(store.load()) }
    var theme by remember { mutableStateOf(prefs.theme()) }
    var sort by remember { mutableStateOf(prefs.sort()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Writes happen off the main thread so editing never waits on the disk.
    val scope = rememberCoroutineScope()
    fun save(updated: List<Note>) {
        notes = updated
        scope.launch(Dispatchers.IO) { store.save(updated) }
    }

    // Commits the open note. A note left empty is discarded rather than kept.
    fun commit(id: Long, title: String, body: String) {
        val blank = title.isBlank() && body.isBlank()
        save(
            if (blank) notes.filterNot { it.id == id }
            else notes.map { if (it.id == id) it.copy(title = title, body = body, updated = System.currentTimeMillis()) else it },
        )
        screen = Screen.Home
    }

    BackHandler(enabled = screen == Screen.Settings) { screen = Screen.Home }

    NotesTheme(theme) {
        // Each direction gets its own motion: the editor grows in from the card, settings slides in
        // from the right, and returning home reverses whichever motion brought you there.
        val spatialInt = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
        val spatialFloat = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    targetState is Screen.Editor ->
                        (fadeIn(effects) + scaleIn(spatialFloat, initialScale = 0.85f)).togetherWith(fadeOut(effects))
                    initialState is Screen.Editor ->
                        fadeIn(effects).togetherWith(fadeOut(effects) + scaleOut(spatialFloat, targetScale = 0.85f))
                    targetState == Screen.Settings ->
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
                Screen.Home -> HomeScreen(
                    notes = notes,
                    sort = sort,
                    onOpen = { screen = Screen.Editor(it) },
                    onNew = {
                        val id = System.currentTimeMillis()
                        save(listOf(Note(id, "", "", id)) + notes)
                        screen = Screen.Editor(id)
                    },
                    onSettings = { screen = Screen.Settings },
                )
                is Screen.Editor -> {
                    val note = notes.firstOrNull { it.id == current.id }
                    if (note == null) {
                        LaunchedEffect(current.id) { screen = Screen.Home }
                    } else {
                        EditorScreen(
                            note = note,
                            onBack = { title, body -> commit(note.id, title, body) },
                            onTogglePin = {
                                save(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it })
                            },
                            onDelete = {
                                save(notes.filterNot { it.id == note.id })
                                screen = Screen.Home
                            },
                        )
                    }
                }
                Screen.Settings -> SettingsScreen(
                    theme = theme,
                    sort = sort,
                    onTheme = {
                        theme = it
                        prefs.setTheme(it)
                    },
                    onSort = {
                        sort = it
                        prefs.setSort(it)
                    },
                    onDeleteAll = { save(emptyList()) },
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreen(
    notes: List<Note>,
    sort: SortOrder,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(notes, query, sort) { notes.visible(query, sort) }
    val pinnedCount = notes.count { it.pinned }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.notes_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (pinnedCount > 0) {
                        Text(
                            text = stringResource(R.string.pinned_count, pinnedCount),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            }

            Spacer(Modifier.height(12.dp))

            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(12.dp))

            if (shown.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (query.isBlank()) EmptyIllustration()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(if (query.isBlank()) R.string.no_notes else R.string.no_results),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(shown, key = { _, note -> note.id }) { index, note ->
                        NoteCard(
                            note = note,
                            shape = segmentShape(index, shown.size),
                            onClick = { onOpen(note.id) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNew,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom))
                .padding(20.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_note)) },
        )
    }
}

@Composable
private fun NoteCard(note: Note, shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterBadge(letter = note.title.firstOrNull()?.uppercaseChar() ?: '•', index = note.id)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title.ifBlank { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (note.body.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatDate(note.updated),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
        }
    }
}

/** A Material 3 Expressive shape drawn on a Canvas, with a letter on top. Colour follows the note. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LetterBadge(letter: Char, index: Long) {
    val scheme = MaterialTheme.colorScheme
    val palette = listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer)
    val color = palette[(index % palette.size).toInt().let { if (it < 0) it + palette.size else it }]
    val shape = MaterialShapes.Cookie9Sided.toShape()
    val layoutDirection = LocalLayoutDirection.current
    Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = color)
        }
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimaryContainer,
        )
    }
}

/** Empty-state art: a large expressive shape with three drawn "text lines" inside it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyIllustration() {
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialShapes.Cookie12Sided.toShape()
    val layoutDirection = LocalLayoutDirection.current
    Canvas(modifier = Modifier.size(140.dp)) {
        drawOutline(outline = shape.createOutline(size, layoutDirection, this), color = scheme.primaryContainer)
        val lineHeight = size.height * 0.06f
        val left = size.width * 0.3f
        val widths = listOf(0.4f, 0.32f, 0.24f)
        widths.forEachIndexed { i, w ->
            val top = size.height * (0.38f + i * 0.14f)
            drawRoundRect(
                color = scheme.onPrimaryContainer,
                topLeft = Offset(left, top),
                size = Size(size.width * w, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditorScreen(
    note: Note,
    onBack: (String, String) -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    // The draft lives here; it is saved only when the note is left, so typing never rewrites the list.
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    BackHandler { onBack(title, body) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = { onBack(title, body) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                placeholder = { Text(stringResource(R.string.note_title_hint)) },
                singleLine = true,
                colors = transparentFieldColors(),
            )
            TextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(stringResource(R.string.note_body_hint)) },
                colors = transparentFieldColors(),
            )
            Spacer(Modifier.height(88.dp))
        }

        FloatingActionButton(
            onClick = { onBack(title, body) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom))
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
        }

        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Bottom))
                .padding(bottom = 16.dp),
        ) {
            IconButton(onClick = onTogglePin) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(if (note.pinned) R.string.unpin else R.string.pin),
                    tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsScreen(
    theme: ThemeMode,
    sort: SortOrder,
    onTheme: (ThemeMode) -> Unit,
    onSort: (SortOrder) -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingsCard(icon = Icons.Filled.Settings, title = stringResource(R.string.setting_theme), shape = segmentShape(0, 3)) {
                ChoiceRow(
                    options = ThemeMode.entries,
                    selected = theme,
                    label = {
                        stringResource(
                            when (it) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        )
                    },
                    onSelect = onTheme,
                )
            }
            SettingsCard(icon = Icons.Filled.Refresh, title = stringResource(R.string.setting_sort), shape = segmentShape(1, 3)) {
                ChoiceRow(
                    options = SortOrder.entries,
                    selected = sort,
                    label = {
                        stringResource(if (it == SortOrder.UPDATED) R.string.sort_updated else R.string.sort_title)
                    },
                    onSelect = onSort,
                )
            }
            Surface(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                shape = segmentShape(2, 3),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.Delete, destructive = true)
                    Spacer(Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.delete_all),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.delete_confirm)) },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    onDeleteAll()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, shape: Shape, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon)
                Spacer(Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, destructive: Boolean = false) {
    val container = if (destructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val content = if (destructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

/** Outer corners of the first and last card are round, inner corners tight, so a list reads as one grouped card. */
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
