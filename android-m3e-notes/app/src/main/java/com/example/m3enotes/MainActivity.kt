package com.example.m3enotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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

/** Screens from the M3E Canvas design, plus the welcome screen shown on first launch. */
private sealed interface Screen {
    data object Welcome : Screen
    data object List : Screen
    data class Note(val id: Long) : Screen
    data object Prefs : Screen
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesApp(store: NoteStore, prefs: Preferences) {
    var notes by remember { mutableStateOf(store.load()) }
    var theme by remember { mutableStateOf(prefs.theme()) }
    var dynamic by remember { mutableStateOf(prefs.dynamicColor()) }
    var sort by remember { mutableStateOf(prefs.sort()) }
    var pinTop by remember { mutableStateOf(prefs.pinnedOnTop()) }
    var screen by remember { mutableStateOf<Screen>(if (prefs.welcomed()) Screen.List else Screen.Welcome) }
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

    NotesTheme(theme, dynamic) {
        AnimatedContent(
            targetState = screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    initialState == Screen.Welcome ->
                        (fadeIn(effects) + slideInVertically(spatialInt) { it / 10 }).togetherWith(fadeOut(effects) + scaleOut(spatialFloat, targetScale = 0.9f))
                    targetState is Screen.Note ->
                        (fadeIn(effects) + scaleIn(spatialFloat, initialScale = 0.85f)).togetherWith(fadeOut(effects))
                    initialState is Screen.Note && targetState == Screen.List && slideDownOnReturn ->
                        (fadeIn(effects) + slideInVertically(spatialInt) { -it / 8 })
                            .togetherWith(fadeOut(effects) + slideOutVertically(spatialInt) { it / 6 })
                    initialState is Screen.Note && targetState == Screen.List ->
                        fadeIn(effects).togetherWith(fadeOut(effects) + scaleOut(spatialFloat, targetScale = 0.85f))
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
                Screen.Welcome -> WelcomeScreen(onStart = {
                    prefs.setWelcomed()
                    screen = Screen.List
                })
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
                    dynamic = dynamic,
                    sort = sort,
                    pinTop = pinTop,
                    onTheme = {
                        theme = it
                        prefs.setTheme(it)
                    },
                    onDynamic = {
                        dynamic = it
                        prefs.setDynamicColor(it)
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
                    onReset = {
                        prefs.resetToDefaults()
                        theme = prefs.theme()
                        dynamic = prefs.dynamicColor()
                        sort = prefs.sort()
                        pinTop = prefs.pinnedOnTop()
                    },
                    onBack = { screen = Screen.List },
                )
            }
        }
    }
}

/** First-launch screen: a large expressive shape, the loading indicator, and the features. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(96.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            LoadingIndicator(modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(24.dp))
            FeatureRow(Icons.Filled.Star, stringResource(R.string.feature_pin))
            FeatureRow(Icons.Filled.Search, stringResource(R.string.feature_search))
            FeatureRow(Icons.Filled.Refresh, stringResource(R.string.feature_colors))
            FeatureRow(Icons.Filled.Share, stringResource(R.string.feature_share))
            Spacer(Modifier.height(96.dp))
        }
        ExtendedFloatingActionButton(
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(24.dp),
            icon = { Icon(Icons.Filled.Done, contentDescription = null) },
            text = { Text(stringResource(R.string.start)) },
        )
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(16.dp))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
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
                    itemsIndexed(shown, key = { _, note -> note.id }) { index, note ->
                        NoteRow(note = note, shape = segmentShape(index, shown.size), onClick = { onOpen(note.id) })
                    }
                }
            }
        }

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
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    val context = LocalContext.current
    BackHandler { onBack(title, body) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBack(title, body) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(
                    text = stringResource(R.string.note_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
            }

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
            IconButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, listOf(title, body).filter { it.isNotBlank() }.joinToString("\n\n"))
                }
                context.startActivity(Intent.createChooser(send, null))
            }) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
            }
        }

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

/** Ayarlar: one column of rounded cards, each with a cookie badge; a FAB restores the defaults. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrefsScreen(
    theme: ThemeChoice,
    dynamic: Boolean,
    sort: SortOrder,
    pinTop: Boolean,
    onTheme: (ThemeChoice) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onSort: (SortOrder) -> Unit,
    onPinTop: (Boolean) -> Unit,
    onDeleteAll: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PrefCard(Icons.Filled.Settings, stringResource(R.string.pref_theme), segmentShape(0, 5)) {
                    ChoiceSelect(
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
                }
                PrefCard(Icons.Filled.Refresh, stringResource(R.string.pref_dynamic), segmentShape(1, 5)) {
                    SwitchRow(stringResource(R.string.pref_dynamic_hint), dynamic, onDynamic)
                }
                PrefCard(Icons.Filled.Done, stringResource(R.string.pref_sort), segmentShape(2, 5)) {
                    ChoiceSelect(
                        current = stringResource(if (sort == SortOrder.UPDATED) R.string.sort_updated else R.string.sort_title),
                        options = SortOrder.entries.map { option ->
                            stringResource(if (option == SortOrder.UPDATED) R.string.sort_updated else R.string.sort_title) to { onSort(option) }
                        },
                    )
                }
                PrefCard(Icons.Filled.Star, stringResource(R.string.pref_pin_top), segmentShape(3, 5)) {
                    SwitchRow(stringResource(R.string.pref_pin_top_hint), pinTop, onPinTop)
                }
                // Destructive card: asks before it deletes everything.
                Surface(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = segmentShape(4, 5),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Delete, destructive = true)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.pref_delete_all),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.pref_reset))
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrefCard(icon: ImageVector, title: String, shape: Shape, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon)
                Spacer(Modifier.width(16.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** An outlined button that opens a menu of options, used for theme and sort. */
@Composable
private fun ChoiceSelect(current: String, options: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
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

/** A cookie-shaped badge with an icon, the same on every card. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IconBadge(icon: ImageVector, destructive: Boolean = false) {
    val container = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer
    val tint = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Outer corners round on the first and last card, inner corners tight: one grouped column. */
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
