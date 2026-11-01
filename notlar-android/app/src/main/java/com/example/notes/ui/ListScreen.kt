@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.example.notes.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.example.notes.Filter
import com.example.notes.NotesViewModel
import com.example.notes.UiState
import com.example.notes.data.Note
import com.example.notes.data.SortOrder
import kotlinx.coroutines.launch

@Composable
fun ListScreen(state: UiState, vm: NotesViewModel, onMenu: () -> Unit) {
    val haptics = rememberHaptics()
    val notes = state.visible
    val gridState = rememberLazyStaggeredGridState()
    var fabOpen by remember { mutableStateOf(false) }
    var colorSheet by remember { mutableStateOf(false) }
    var labelSheet by remember { mutableStateOf(false) }
    var confirmEmpty by remember { mutableStateOf(false) }
    val selecting = state.selection.isNotEmpty()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 } }
    val fabVisible = !selecting && state.filter != Filter.Trash && state.filter != Filter.Archive

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize()) {
            val fastFx = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            val fastSp = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
            AnimatedContent(
                targetState = selecting,
                transitionSpec = { (fadeIn(fastFx) + scaleIn(fastSp, 0.92f)) togetherWith fadeOut(fastFx) },
                label = "topbar",
            ) { sel ->
                if (sel) {
                    SelectionBar(
                        state = state,
                        onClose = { haptics.tick(); vm.clearSelection() },
                        onPin = { haptics.toggle(true); vm.togglePin(state.selection) },
                        onColor = { colorSheet = true },
                        onLabel = { labelSheet = true },
                        onArchive = { haptics.confirm(); vm.archive(state.selection, state.filter != Filter.Archive) },
                        onTrash = { haptics.long(); vm.trash(state.selection) },
                        onRestore = { haptics.confirm(); vm.restoreFromTrash(state.selection) },
                        onDeleteForever = { haptics.long(); vm.deleteForever(state.selection) },
                        onSelectAll = { haptics.tick(); vm.selectAll() },
                        onDuplicate = { state.selection.singleOrNull()?.let(vm::duplicate) },
                    )
                } else {
                    SearchHeader(
                        query = state.query,
                        grid = state.settings.grid,
                        sort = state.settings.sort,
                        elevated = scrolled,
                        onQuery = vm::setQuery,
                        onMenu = { haptics.tick(); onMenu() },
                        onToggleGrid = {
                            haptics.toggle(!state.settings.grid)
                            vm.updateSettings { it.copy(grid = !it.grid) }
                        },
                        onSort = { s -> haptics.segment(); vm.updateSettings { it.copy(sort = s) } },
                    )
                }
            }

            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Adaptive(if (state.settings.grid) 160.dp else 600.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 120.dp),
                verticalItemSpacing = 10.dp,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = StaggeredGridItemSpan.FullLine, key = "header") {
                    FilterHeader(
                        state = state,
                        count = notes.size,
                        onLabel = { vm.setFilter(if (state.filter == Filter.Label(it)) Filter.Notes else Filter.Label(it)); haptics.segment() },
                        onEmptyTrash = { confirmEmpty = true },
                    )
                }
                if (notes.isEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "empty") {
                        EmptyState(state.filter, state.query.isNotBlank())
                    }
                }
                val pinned = notes.filter { it.pinned }
                val others = notes.filterNot { it.pinned }
                if (pinned.isNotEmpty() && state.filter != Filter.Trash) {
                    item(span = StaggeredGridItemSpan.FullLine, key = "pinned-h") { SectionLabel("Sabitlenenler", Icons.Filled.PushPin) }
                    items(pinned, key = { it.id }) { note -> NoteItem(note, state, vm) }
                    if (others.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine, key = "others-h") { SectionLabel("Diğerleri", null) }
                    }
                }
                items(if (state.filter == Filter.Trash) notes else others, key = { it.id }) { note -> NoteItem(note, state, vm) }
            }
        }

        // Expressive FAB menu: a text note or a checklist.
        AnimatedVisibility(
            visible = fabVisible,
            enter = scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 8.dp),
        ) {
            FloatingActionButtonMenu(
                expanded = fabOpen,
                button = {
                    ToggleFloatingActionButton(
                        checked = fabOpen,
                        onCheckedChange = { haptics.toggle(it); fabOpen = it },
                        modifier = Modifier.semantics { contentDescription = if (fabOpen) "Menüyü kapat" else "Yeni not" },
                    ) {
                        val progress = checkedProgress
                        Icon(
                            if (progress > 0.5f) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = null,
                            modifier = with(ToggleFloatingActionButtonDefaults) { Modifier.animateIcon({ checkedProgress }) },
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = { haptics.confirm(); fabOpen = false; vm.create(checklist = true) },
                    text = { Text("Liste") },
                    icon = { Icon(Icons.Outlined.Checklist, null) },
                )
                FloatingActionButtonMenuItem(
                    onClick = { haptics.confirm(); fabOpen = false; vm.create() },
                    text = { Text("Metin notu") },
                    icon = { Icon(Icons.Outlined.Edit, null) },
                )
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = fabOpen) { fabOpen = false }

    if (colorSheet) {
        val current = state.notes.filter { it.id in state.selection }.map { it.color }.distinct().singleOrNull() ?: -1
        ColorSheet(selected = current, onPick = { haptics.segment(); vm.setColor(state.selection, it) }, onDismiss = { colorSheet = false })
    }
    if (labelSheet) {
        LabelPickerSheet(
            labels = state.allLabels,
            notes = state.notes.filter { it.id in state.selection },
            onToggle = { haptics.segment(); vm.toggleLabel(state.selection, it) },
            onCreate = { vm.addLabel(it); vm.toggleLabel(state.selection, it.trim()) },
            onDismiss = { labelSheet = false },
        )
    }
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            icon = { Icon(Icons.Outlined.DeleteForever, null) },
            title = { Text("Çöp kutusu boşaltılsın mı?") },
            text = { Text("Çöp kutusundaki tüm notlar kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = { haptics.long(); vm.emptyTrash(); confirmEmpty = false }) { Text("Boşalt") }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun SearchHeader(
    query: String,
    grid: Boolean,
    sort: SortOrder,
    elevated: Boolean,
    onQuery: (String) -> Unit,
    onMenu: () -> Unit,
    onToggleGrid: () -> Unit,
    onSort: (SortOrder) -> Unit,
) {
    val container by androidx.compose.animation.animateColorAsState(
        if (elevated) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "search",
    )
    var sortMenu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .height(56.dp)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenu) { Icon(Icons.Outlined.Menu, "Menü") }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("Notlarda ara", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Notlarda ara" },
            )
        }
        AnimatedContent(query.isNotEmpty(), label = "clear") { has ->
            if (has) IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, "Aramayı temizle") }
            else Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Box {
            IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Outlined.Sort, "Sırala") }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }, shape = MaterialTheme.shapes.large) {
                listOf(SortOrder.Updated to "Son düzenlenen", SortOrder.Created to "Oluşturulma tarihi", SortOrder.Title to "Başlık (A–Z)").forEach { (s, label) ->
                    DropdownMenuItem(
                        text = { Text(label, fontWeight = if (s == sort) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onSort(s); sortMenu = false },
                        trailingIcon = { if (s == sort) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
                    )
                }
            }
        }
        IconButton(onClick = onToggleGrid, shapes = IconButtonDefaults.shapes()) {
            AnimatedContent(grid, label = "grid") { g ->
                Icon(if (g) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView, if (g) "Liste görünümü" else "Izgara görünümü")
            }
        }
    }
}

@Composable
private fun FilterHeader(state: UiState, count: Int, onLabel: (String) -> Unit, onEmptyTrash: () -> Unit) {
    val (title, icon) = when (val f = state.filter) {
        Filter.Notes -> "Notlar" to Icons.Outlined.Lightbulb
        Filter.Reminders -> "Hatırlatıcılar" to Icons.Outlined.Notifications
        Filter.Archive -> "Arşiv" to Icons.Outlined.Archive
        Filter.Trash -> "Çöp kutusu" to Icons.Outlined.Delete
        is Filter.Label -> f.name to Icons.AutoMirrored.Outlined.Label
    }
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(12.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        if (state.filter == Filter.Trash) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Çöp kutusundaki notlar 7 gün sonra silinir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (count > 0) FilledTonalButton(onClick = onEmptyTrash) { Text("Boşalt") }
            }
        }
        val labels = state.allLabels
        if (labels.isNotEmpty() && (state.filter == Filter.Notes || state.filter is Filter.Label)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                items(labels, key = { it }) { label ->
                    val selected = state.filter == Filter.Label(label)
                    FilterChip(
                        selected = selected,
                        onClick = { onLabel(label) },
                        label = { Text(label) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null, Modifier.size(FilterChipDefaults.IconSize)) },
                        shape = CircleShape,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector?) {
    Row(Modifier.padding(start = 10.dp, top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
        }
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope.NoteItem(note: Note, state: UiState, vm: NotesViewModel) {
    val haptics = rememberHaptics()
    val selecting = state.selection.isNotEmpty()
    val inTrash = note.inTrash
    val swipe = rememberSwipeToDismissBoxState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // A dismissed box stays dismissed; bring it back so a restored or still-listed note shows again.
    androidx.compose.runtime.LaunchedEffect(Unit) { if (swipe.currentValue != SwipeToDismissBoxValue.Settled) swipe.reset() }
    Box(Modifier.animateItem(fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(), placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(), fadeOutSpec = MaterialTheme.motionScheme.fastEffectsSpec())) {
        SwipeToDismissBox(
            state = swipe,
            gesturesEnabled = !selecting && !inTrash,
            onDismiss = { value ->
                haptics.confirm()
                scope.launch { swipe.reset() }
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> vm.archive(listOf(note.id), !note.archived)
                    SwipeToDismissBoxValue.EndToStart -> vm.trash(listOf(note.id))
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            },
            backgroundContent = { SwipeBackground(swipe.dismissDirection, note.archived, swipe.progress) },
        ) {
            NoteCard(
                note = note,
                selected = note.id in state.selection,
                previewLines = state.settings.showPreviewLines,
                onClick = {
                    if (selecting) { haptics.segment(); vm.toggleSelect(note.id) }
                    else if (inTrash) { haptics.reject() }
                    else { haptics.tick(); vm.open(note.id) }
                },
                onLongClick = { haptics.long(); vm.toggleSelect(note.id) },
            )
        }
    }
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, archived: Boolean, progress: Float) {
    val toArchive = direction == SwipeToDismissBoxValue.StartToEnd
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    val iconScale by animateFloatAsState(if (progress > 0.35f) 1.25f else 0.9f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "icon")
    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(color).padding(horizontal = 24.dp),
        contentAlignment = if (toArchive) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(
                when {
                    !toArchive -> Icons.Outlined.Delete
                    archived -> Icons.Outlined.Unarchive
                    else -> Icons.Outlined.Archive
                },
                contentDescription = null,
                modifier = Modifier.scale(iconScale),
                tint = if (toArchive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
fun NoteCard(note: Note, selected: Boolean, previewLines: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "press")
    val corner by animateDpAsState(if (selected) 36.dp else if (pressed) 32.dp else 24.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "corner")
    val tint = noteContainer(note.color)
    val container = tint ?: MaterialTheme.colorScheme.surfaceContainerLow
    val content = noteContent(note.color)
    val shared = LocalSharedScopes.current
    val sharedModifier = if (shared != null) with(shared.transition) {
        Modifier.sharedBounds(
            rememberSharedContentState("note-${note.id}"),
            animatedVisibilityScope = shared.visibility,
            resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    } else Modifier

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(corner),
        border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            tint == null -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            else -> null
        },
        modifier = sharedModifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .combinedClickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick, onLongClick = onLongClick),
    ) {
        Box {
            Column(Modifier.padding(16.dp)) {
                if (note.title.isNotBlank()) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = if (note.pinned) 20.dp else 0.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                }
                if (note.isChecklist) {
                    val shown = note.items.filter { it.text.isNotBlank() }.sortedBy { it.checked }.take(previewLines)
                    shown.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                            Icon(
                                if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                null,
                                Modifier.size(16.dp),
                                tint = content.copy(alpha = if (item.checked) 0.5f else 0.8f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                color = content.copy(alpha = if (item.checked) 0.55f else 0.85f),
                            )
                        }
                    }
                    val more = note.items.count { it.text.isNotBlank() } - shown.size
                    if (more > 0) Text("+$more öğe", style = MaterialTheme.typography.labelMedium, color = content.copy(alpha = 0.6f))
                } else if (note.body.isNotBlank()) {
                    Text(
                        note.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = previewLines,
                        overflow = TextOverflow.Ellipsis,
                        color = content.copy(alpha = 0.82f),
                    )
                }
                if (note.title.isBlank() && note.isBlank) {
                    Text("Boş not", style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.5f))
                }
                if (note.labels.isNotEmpty() || note.reminder != null) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        note.reminder?.let { r ->
                            MiniChip(Icons.Outlined.Alarm, formatReminder(r), content, strike = r < System.currentTimeMillis())
                        }
                        note.labels.take(3).forEach { MiniChip(null, it, content) }
                        if (note.labels.size > 3) MiniChip(null, "+${note.labels.size - 3}", content)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    DateUtils.getRelativeTimeSpanString(note.updated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tint == null) MaterialTheme.colorScheme.primary else content.copy(alpha = 0.6f),
                )
            }
            if (note.pinned) {
                Icon(Icons.Filled.PushPin, "Sabitlendi", Modifier.align(Alignment.TopEnd).padding(12.dp).size(18.dp), tint = content.copy(alpha = 0.7f))
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()),
                exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()),
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            ) {
                Surface(shape = PolygonShape(MaterialShapes.Cookie6Sided), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniChip(icon: ImageVector?, text: String, content: Color, strike: Boolean = false) {
    Row(
        Modifier.clip(CircleShape).background(content.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(12.dp), tint = content.copy(alpha = 0.75f))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.8f),
            maxLines = 1,
            textDecoration = if (strike) TextDecoration.LineThrough else null,
        )
    }
}

fun formatReminder(time: Long): String {
    val clock = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(time)
    return when {
        DateUtils.isToday(time) -> "Bugün $clock"
        DateUtils.isToday(time - DateUtils.DAY_IN_MILLIS) -> "Yarın $clock"
        else -> java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.getDefault()).format(time)
    }
}

@Composable
private fun SelectionBar(
    state: UiState,
    onClose: () -> Unit,
    onPin: () -> Unit,
    onColor: () -> Unit,
    onLabel: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onSelectAll: () -> Unit,
    onDuplicate: () -> Unit,
) {
    var more by remember { mutableStateOf(false) }
    val trash = state.filter == Filter.Trash
    TopAppBar(
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Seçimi kapat") } },
        title = {
            AnimatedContent(state.selection.size, label = "count", transitionSpec = {
                (fadeIn() + androidx.compose.animation.slideInVertically { if (targetState > initialState) it else -it }) togetherWith
                    (fadeOut() + androidx.compose.animation.slideOutVertically { if (targetState > initialState) -it else it })
            }) { n -> Text("$n seçildi", fontWeight = FontWeight.Bold) }
        },
        actions = {
            if (trash) {
                IconButton(onClick = onRestore) { Icon(Icons.Outlined.RestoreFromTrash, "Geri yükle") }
                IconButton(onClick = onDeleteForever) { Icon(Icons.Outlined.DeleteForever, "Kalıcı olarak sil") }
            } else {
                val allPinned = state.notes.filter { it.id in state.selection }.all { it.pinned }
                IconButton(onClick = onPin) { Icon(if (allPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, if (allPinned) "Sabitlemeyi kaldır" else "Sabitle") }
                IconButton(onClick = onColor) { Icon(Icons.Outlined.Palette, "Renk") }
                IconButton(onClick = onLabel) { Icon(Icons.AutoMirrored.Outlined.Label, "Etiket") }
                IconButton(onClick = onArchive) {
                    Icon(if (state.filter == Filter.Archive) Icons.Outlined.Unarchive else Icons.Outlined.Archive, "Arşivle")
                }
            }
            Box {
                IconButton(onClick = { more = true }) { Icon(Icons.Outlined.MoreVert, "Diğer") }
                DropdownMenu(more, onDismissRequest = { more = false }, shape = MaterialTheme.shapes.large) {
                    DropdownMenuItem(text = { Text("Tümünü seç") }, leadingIcon = { Icon(Icons.Outlined.SelectAll, null) }, onClick = { more = false; onSelectAll() })
                    if (!trash) {
                        if (state.selection.size == 1) {
                            DropdownMenuItem(text = { Text("Kopyasını oluştur") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { more = false; onDuplicate() })
                        }
                        DropdownMenuItem(text = { Text("Sil") }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { more = false; onTrash() })
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

private val emptyMorph by lazy { Morph(MaterialShapes.Cookie12Sided, MaterialShapes.Flower) }

@Composable
private fun EmptyState(filter: Filter, searching: Boolean) {
    val infinite = rememberInfiniteTransition(label = "empty")
    val morph by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "morph")
    val rotation by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = androidx.compose.animation.core.LinearEasing)), label = "spin")
    val (icon, title, subtitle) = when {
        searching -> Triple(Icons.Outlined.Search, "Eşleşen not yok", "Başka bir kelime deneyin.")
        filter == Filter.Trash -> Triple(Icons.Outlined.Delete, "Çöp kutusu boş", "Silinen notlar burada 7 gün kalır.")
        filter == Filter.Archive -> Triple(Icons.Outlined.Archive, "Arşiv boş", "Arşivlediğiniz notlar burada görünür.")
        filter == Filter.Reminders -> Triple(Icons.Outlined.Notifications, "Hatırlatıcı yok", "Bir nota hatırlatıcı ekleyin.")
        filter is Filter.Label -> Triple(Icons.AutoMirrored.Outlined.Label, "Bu etikette not yok", "Yeni not bu etiketle oluşturulur.")
        else -> Triple(Icons.Outlined.Lightbulb, "Henüz not yok", "Yeni not için + simgesine dokunun.")
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(148.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .clip(MorphShape(emptyMorph, morph))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}
