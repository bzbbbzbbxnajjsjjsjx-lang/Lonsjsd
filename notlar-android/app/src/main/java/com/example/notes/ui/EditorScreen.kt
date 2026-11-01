@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.notes.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.NotesViewModel
import com.example.notes.UiState
import com.example.notes.data.CheckItem
import com.example.notes.data.Note

@Composable
fun EditorScreen(note: Note, state: UiState, vm: NotesViewModel) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val (canUndo, canRedo) = vm.history.collectAsStateWithLifecycle().value
    var colorSheet by remember { mutableStateOf(false) }
    var labelSheet by remember { mutableStateOf(false) }
    var reminderSheet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val scale = state.settings.textScale

    val tint = noteContainer(note.color)
    val container by animateColorAsState(tint ?: MaterialTheme.colorScheme.surface, MaterialTheme.motionScheme.slowEffectsSpec(), label = "bg")
    val content = noteContent(note.color)

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun close() { haptics.tick(); vm.close() }
    BackHandler { close() }

    fun share() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, note.plainText())
        }
        context.startActivity(Intent.createChooser(send, "Notu paylaş"))
    }

    val shared = LocalSharedScopes.current
    val sharedModifier = if (shared != null) with(shared.transition) {
        Modifier.sharedBounds(
            rememberSharedContentState("note-${note.id}"),
            animatedVisibilityScope = shared.visibility,
            resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    } else Modifier

    Surface(color = container, contentColor = content, modifier = sharedModifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().imePadding()) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TipIconButton("Geri", Icons.AutoMirrored.Filled.ArrowBack) { close() }
                    Spacer(Modifier.weight(1f))
                    IconToggleButton(
                        checked = note.pinned,
                        onCheckedChange = { haptics.toggle(it); vm.togglePin(listOf(note.id)) },
                        shapes = IconButtonDefaults.toggleableShapes(),
                    ) {
                        val rot by animateFloatAsState(if (note.pinned) 0f else 45f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "pin")
                        Icon(
                            if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            if (note.pinned) "Sabitlemeyi kaldır" else "Sabitle",
                            Modifier.graphicsLayer { rotationZ = rot },
                        )
                    }
                    TipIconButton(if (note.reminder != null) "Hatırlatıcıyı düzenle" else "Hatırlatıcı ekle", Icons.Outlined.AddAlert) {
                        haptics.tick()
                        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        reminderSheet = true
                    }
                    TipIconButton(if (note.archived) "Arşivden çıkar" else "Arşivle", if (note.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive) {
                        haptics.confirm(); vm.archive(listOf(note.id), !note.archived)
                    }
                    Box {
                        TipIconButton("Diğer", Icons.Outlined.MoreVert) { menu = true }
                        DropdownMenu(menu, onDismissRequest = { menu = false }, shape = MaterialTheme.shapes.large) {
                            DropdownMenuItem(text = { Text("Paylaş") }, leadingIcon = { Icon(Icons.Outlined.Share, null) }, onClick = { menu = false; share() })
                            DropdownMenuItem(text = { Text("Kopyasını oluştur") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { menu = false; vm.duplicate(note.id) })
                            DropdownMenuItem(text = { Text("Etiketler") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, null) }, onClick = { menu = false; labelSheet = true })
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Sil", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; haptics.long(); vm.trash(listOf(note.id)) },
                            )
                        }
                    }
                }

                EditorBody(note = note, scale = scale, content = content, vm = vm, onLabels = { labelSheet = true }, onReminder = { reminderSheet = true })
            }

            // Expressive floating toolbar with the main editing tools.
            HorizontalFloatingToolbar(
                expanded = true,
                colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = { haptics.tick(); share() }) {
                        Icon(Icons.Outlined.Share, "Paylaş")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 12.dp)
                    .offset(y = 0.dp),
            ) {
                TipIconButton("Renk", Icons.Outlined.Palette) { haptics.tick(); colorSheet = true }
                TipIconButton(if (note.isChecklist) "Metne dönüştür" else "Listeye dönüştür", if (note.isChecklist) Icons.Outlined.Notes else Icons.Outlined.Checklist) {
                    haptics.toggle(!note.isChecklist); vm.edit { it.toggledChecklist() }
                }
                TipIconButton("Etiketler", Icons.AutoMirrored.Outlined.Label) { haptics.tick(); labelSheet = true }
                TipIconButton("Geri al", Icons.AutoMirrored.Filled.Undo, enabled = canUndo) { haptics.tick(); vm.undo() }
                TipIconButton("Yinele", Icons.AutoMirrored.Filled.Redo, enabled = canRedo) { haptics.tick(); vm.redo() }
            }
        }
    }

    if (colorSheet) ColorSheet(note.color, onPick = { haptics.segment(); vm.edit { n -> n.copy(color = it) } }, onDismiss = { colorSheet = false })
    if (labelSheet) LabelPickerSheet(
        labels = state.allLabels,
        notes = listOf(note),
        onToggle = { haptics.segment(); vm.edit { n -> n.copy(labels = if (it in n.labels) n.labels - it else n.labels + it) } },
        onCreate = { label -> vm.addLabel(label); vm.edit { n -> n.copy(labels = (n.labels + label.trim()).distinct()) } },
        onDismiss = { labelSheet = false },
    )
    if (reminderSheet) ReminderSheet(note.reminder, onSet = { haptics.confirm(); vm.setReminder(note.id, it); reminderSheet = false }, onDismiss = { reminderSheet = false })
}

@Composable
private fun EditorBody(note: Note, scale: Float, content: Color, vm: NotesViewModel, onLabels: () -> Unit, onReminder: () -> Unit) {
    val haptics = rememberHaptics()
    val listState = rememberLazyListState()
    val titleStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = content)
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(color = content, fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * scale)
    val cursor = SolidColor(MaterialTheme.colorScheme.primary)
    val bodyFocus = remember { FocusRequester() }
    var focusItem by remember { mutableStateOf<String?>(null) }
    var showChecked by remember { mutableStateOf(true) }

    // A brand-new empty note puts the cursor straight into the body.
    LaunchedEffect(note.id) {
        if (note.isBlank && !note.isChecklist) runCatching { bodyFocus.requestFocus() }
        if (note.isBlank && note.isChecklist) focusItem = note.items.firstOrNull()?.id
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 140.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title") {
            Box {
                if (note.title.isEmpty()) Text("Başlık", style = titleStyle.copy(color = content.copy(alpha = 0.4f)))
                SyncedTextField(
                    value = note.title,
                    onValueChange = { v -> vm.edit { it.copy(title = v.replace("\n", " ")) } },
                    textStyle = titleStyle,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        if (note.isChecklist) focusItem = note.items.firstOrNull()?.id else runCatching { bodyFocus.requestFocus() }
                    }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Başlık" },
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!note.isChecklist) {
            item(key = "body") {
                Box(Modifier.fillMaxWidth()) {
                    if (note.body.isEmpty()) Text("Notunuzu yazın", style = bodyStyle.copy(color = content.copy(alpha = 0.4f)))
                    SyncedTextField(
                        value = note.body,
                        onValueChange = { v -> vm.edit { it.copy(body = v) } },
                        textStyle = bodyStyle,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().focusRequester(bodyFocus).semantics { contentDescription = "Not metni" },
                    )
                }
            }
        } else {
            val open = note.items.filter { !it.checked }
            val done = note.items.filter { it.checked }
            items(open, key = { "i-" + it.id }) { item ->
                ChecklistRow(
                    item = item, style = bodyStyle, content = content, focus = focusItem == item.id,
                    onFocused = { focusItem = null },
                    onChange = { text -> vm.edit { n -> n.copy(items = n.items.map { if (it.id == item.id) it.copy(text = text) else it }) } },
                    onCheck = { c -> haptics.toggle(c); vm.edit { n -> n.copy(items = n.items.map { if (it.id == item.id) it.copy(checked = c) else it }) } },
                    onNext = {
                        val fresh = CheckItem()
                        vm.edit { n ->
                            val idx = n.items.indexOfFirst { it.id == item.id }
                            n.copy(items = n.items.toMutableList().apply { add(idx + 1, fresh) })
                        }
                        focusItem = fresh.id
                    },
                    onRemove = {
                        val idx = open.indexOf(item)
                        vm.edit { n -> n.copy(items = n.items.filterNot { it.id == item.id }) }
                        focusItem = open.getOrNull(idx - 1)?.id
                    },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "add") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .clickable {
                            haptics.tick()
                            val fresh = CheckItem()
                            vm.edit { n -> n.copy(items = n.items.filterNot { it.checked } + fresh + n.items.filter { it.checked }) }
                            focusItem = fresh.id
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, null, tint = content.copy(alpha = 0.7f))
                    Spacer(Modifier.width(16.dp))
                    Text("Öğe ekle", style = bodyStyle.copy(color = content.copy(alpha = 0.7f)))
                }
            }
            if (done.isNotEmpty()) {
                item(key = "done-h") {
                    val rot by animateFloatAsState(if (showChecked) 0f else -90f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "chev")
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape).clickable { haptics.tick(); showChecked = !showChecked }.padding(12.dp).animateItem(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.ExpandMore, null, Modifier.graphicsLayer { rotationZ = rot }, tint = content.copy(alpha = 0.7f))
                        Spacer(Modifier.width(12.dp))
                        Text("${done.size} işaretlenmiş öğe", style = MaterialTheme.typography.titleSmall, color = content.copy(alpha = 0.7f))
                    }
                }
                if (showChecked) {
                    items(done, key = { "d-" + it.id }) { item ->
                        ChecklistRow(
                            item = item, style = bodyStyle, content = content, focus = false, onFocused = {},
                            onChange = { text -> vm.edit { n -> n.copy(items = n.items.map { if (it.id == item.id) it.copy(text = text) else it }) } },
                            onCheck = { c -> haptics.toggle(c); vm.edit { n -> n.copy(items = n.items.map { if (it.id == item.id) it.copy(checked = c) else it }) } },
                            onNext = {},
                            onRemove = { vm.edit { n -> n.copy(items = n.items.filterNot { it.id == item.id }) } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        item(key = "meta") {
            Spacer(Modifier.height(20.dp))
            if (note.labels.isNotEmpty() || note.reminder != null) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    note.reminder?.let { r ->
                        InputChip(
                            selected = false,
                            onClick = onReminder,
                            label = { Text(formatReminder(r), textDecoration = if (r < System.currentTimeMillis()) TextDecoration.LineThrough else null) },
                            leadingIcon = { Icon(Icons.Outlined.Alarm, null, Modifier.size(InputChipDefaults.IconSize)) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, "Hatırlatıcıyı kaldır", Modifier.size(InputChipDefaults.IconSize).clickable { vm.setReminder(note.id, null) })
                            },
                            shape = CircleShape,
                        )
                    }
                    note.labels.forEach { label ->
                        InputChip(
                            selected = false,
                            onClick = onLabels,
                            label = { Text(label) },
                            trailingIcon = {
                                Icon(Icons.Filled.Close, "$label etiketini kaldır", Modifier.size(InputChipDefaults.IconSize).clickable {
                                    vm.edit { n -> n.copy(labels = n.labels - label) }
                                })
                            },
                            shape = CircleShape,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            val text = note.plainText()
            val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
            Text(
                "Düzenlendi " + DateUtils.getRelativeTimeSpanString(note.updated, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS) +
                    " · $words kelime · ${text.length} karakter",
                style = MaterialTheme.typography.labelMedium,
                color = content.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    item: CheckItem,
    style: TextStyle,
    content: Color,
    focus: Boolean,
    onFocused: () -> Unit,
    onChange: (String) -> Unit,
    onCheck: (Boolean) -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requester = remember { FocusRequester() }
    LaunchedEffect(focus) {
        if (focus) { runCatching { requester.requestFocus() }; onFocused() }
    }
    val alpha by animateFloatAsState(if (item.checked) 0.5f else 1f, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "a")
    Row(modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.DragIndicator, null, Modifier.size(18.dp), tint = content.copy(alpha = 0.25f))
        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheck,
            colors = CheckboxDefaults.colors(uncheckedColor = content.copy(alpha = 0.6f)),
        )
        Box(Modifier.weight(1f)) {
            if (item.text.isEmpty()) Text("Liste öğesi", style = style.copy(color = content.copy(alpha = 0.35f)))
            SyncedTextField(
                value = item.text,
                onValueChange = { v ->
                    // Enter splits into a new item rather than a line break.
                    if (v.contains('\n')) { onChange(v.replace("\n", "")); onNext() } else onChange(v)
                },
                textStyle = style.copy(
                    color = content.copy(alpha = alpha),
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(requester)
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace && item.text.isEmpty()) { onRemove(); true } else false
                    },
            )
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Öğeyi sil", tint = content.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) }
    }
}

@Composable
fun TipIconButton(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled, shapes = IconButtonDefaults.shapes()) { Icon(icon, label) }
    }
}


/**
 * A text field that owns its own text and cursor, so typing never waits for the view model
 * round trip. Changes from outside (undo, redo, checklist conversion) still replace the text;
 * echoes of what this field itself sent are ignored.
 */
@Composable
fun SyncedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var field by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))) }
    val sent = remember { ArrayDeque<String>() }
    var lastExternal by remember { mutableStateOf(value) }
    if (value != lastExternal) {
        lastExternal = value
        if (value in sent) {
            while (sent.isNotEmpty() && sent.removeFirst() != value) Unit
        } else if (value != field.text) {
            sent.clear()
            field = androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))
        }
    }
    BasicTextField(
        value = field,
        onValueChange = { v ->
            val textChanged = v.text != field.text
            field = v
            if (textChanged) {
                sent.addLast(v.text)
                if (sent.size > 64) sent.removeFirst()
                onValueChange(v.text)
            }
        },
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier,
    )
}
