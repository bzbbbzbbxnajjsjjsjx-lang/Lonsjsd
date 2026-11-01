@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
)

package com.example.notes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.Filter
import com.example.notes.NotesViewModel
import kotlinx.coroutines.launch

private enum class Screen { List, Editor, Settings }

/** Scopes that let a card and its editor share bounds during the open/close transition. */
class SharedScopes(val transition: SharedTransitionScope, val visibility: AnimatedVisibilityScope)

val LocalSharedScopes = staticCompositionLocalOf<SharedScopes?> { null }

@Composable
fun NotesApp(vm: NotesViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val haptics = rememberHaptics()
    var labelEditor by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.snackbar.collect { snack ->
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = snack.text,
                actionLabel = if (snack.undo != null) "Geri al" else null,
                withDismissAction = snack.undo == null,
                duration = if (snack.undo != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                haptics.confirm()
                snack.undo?.invoke()
            }
        }
    }

    val screen = when {
        state.editingId != null && state.editing != null -> Screen.Editor
        state.showSettings -> Screen.Settings
        else -> Screen.List
    }

    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
    BackHandler(enabled = !drawer.isOpen && screen == Screen.List && state.selection.isNotEmpty()) { vm.clearSelection() }
    BackHandler(enabled = !drawer.isOpen && screen == Screen.List && state.filter != Filter.Notes && state.selection.isEmpty()) {
        vm.setFilter(Filter.Notes)
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        gesturesEnabled = screen == Screen.List,
        drawerContent = {
            AppDrawer(
                filter = state.filter,
                settingsOpen = state.showSettings,
                labels = state.allLabels,
                counts = DrawerCounts(
                    notes = state.notes.count { !it.archived && !it.inTrash },
                    reminders = state.notes.count { it.reminder != null && !it.inTrash },
                    archive = state.notes.count { it.archived && !it.inTrash },
                    trash = state.notes.count { it.inTrash },
                ),
                onFilter = {
                    haptics.segment()
                    vm.setFilter(it)
                    scope.launch { drawer.close() }
                },
                onSettings = {
                    haptics.segment()
                    vm.openSettings(true)
                    scope.launch { drawer.close() }
                },
                onEditLabels = {
                    labelEditor = true
                    scope.launch { drawer.close() }
                },
            )
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        ) { _ ->
            SharedTransitionLayout {
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                val scale = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                AnimatedContent(
                    targetState = screen,
                    label = "screen",
                    transitionSpec = {
                        when {
                            targetState == Screen.Settings -> (slideInHorizontally(spatial) { it / 3 } + fadeIn(effects)) togetherWith
                                (slideOutHorizontally(spatial) { -it / 6 } + fadeOut(effects))
                            initialState == Screen.Settings -> (slideInHorizontally(spatial) { -it / 6 } + fadeIn(effects)) togetherWith
                                (slideOutHorizontally(spatial) { it / 3 } + fadeOut(effects))
                            else -> (fadeIn(effects) + scaleIn(scale, initialScale = 0.96f)) togetherWith
                                (fadeOut(effects) + scaleOut(scale, targetScale = 0.98f))
                        }
                    },
                ) { target ->
                    val scopes = SharedScopes(this@SharedTransitionLayout, this@AnimatedContent)
                    androidx.compose.runtime.CompositionLocalProvider(LocalSharedScopes provides scopes) {
                        when (target) {
                            Screen.List -> ListScreen(
                                state = state,
                                vm = vm,
                                onMenu = { scope.launch { drawer.open() } },
                            )
                            Screen.Editor -> {
                                // Keep the last note on screen while the close animation runs.
                                val note = state.editing
                                var last by remember { mutableStateOf(note) }
                                if (note != null) last = note
                                last?.let { EditorScreen(note = it, state = state, vm = vm) }
                            }
                            Screen.Settings -> SettingsScreen(state = state, vm = vm, onBack = { vm.openSettings(false) })
                        }
                    }
                }
            }
        }
    }

    if (labelEditor) {
        LabelManagerSheet(
            labels = state.allLabels,
            onAdd = vm::addLabel,
            onRename = vm::renameLabel,
            onDelete = vm::deleteLabel,
            onDismiss = { labelEditor = false },
        )
    }
}

data class DrawerCounts(val notes: Int, val reminders: Int, val archive: Int, val trash: Int)

@Composable
private fun AppDrawer(
    filter: Filter,
    settingsOpen: Boolean,
    labels: List<String>,
    counts: DrawerCounts,
    onFilter: (Filter) -> Unit,
    onSettings: () -> Unit,
    onEditLabels: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.padding(start = 16.dp, top = 28.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = PolygonShape(MaterialShapes.Cookie9Sided),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.size(14.dp))
                Text("Notlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            DrawerItem("Notlar", Icons.Outlined.Lightbulb, counts.notes, !settingsOpen && filter == Filter.Notes) { onFilter(Filter.Notes) }
            DrawerItem("Hatırlatıcılar", Icons.Outlined.Notifications, counts.reminders, !settingsOpen && filter == Filter.Reminders) { onFilter(Filter.Reminders) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Etiketler", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.TextButton(onClick = onEditLabels) { Text("Düzenle") }
            }
            labels.forEach { label ->
                DrawerItem(label, Icons.AutoMirrored.Outlined.Label, null, !settingsOpen && filter == Filter.Label(label)) { onFilter(Filter.Label(label)) }
            }
            if (labels.isEmpty()) {
                DrawerItem("Etiket oluştur", Icons.Outlined.Edit, null, false, onEditLabels)
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            DrawerItem("Arşiv", Icons.Outlined.Archive, counts.archive, !settingsOpen && filter == Filter.Archive) { onFilter(Filter.Archive) }
            DrawerItem("Çöp kutusu", Icons.Outlined.Delete, counts.trash, !settingsOpen && filter == Filter.Trash) { onFilter(Filter.Trash) }
            DrawerItem("Ayarlar", Icons.Outlined.Settings, null, settingsOpen, onSettings)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int?, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1) },
        icon = { Icon(icon, null) },
        badge = { if (count != null && count > 0) Text(count.toString()) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(vertical = 2.dp).clip(MaterialTheme.shapes.extraLarge),
    )
}
