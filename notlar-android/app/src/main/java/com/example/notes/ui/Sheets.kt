@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.notes.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.notes.data.Note
import java.util.Calendar

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

/** Color palette as morphing swatches: the chosen color becomes a scalloped cookie. */
@Composable
fun ColorSheet(selected: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetTitle("Renk")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding().padding(bottom = 16.dp),
        ) {
            NotePalette.forEachIndexed { index, tint ->
                ColorSwatch(index, tint, index == selected) { onPick(index) }
            }
        }
    }
}

@Composable
fun ColorSwatch(index: Int, tint: NoteTint, selected: Boolean, size: androidx.compose.ui.unit.Dp = 52.dp, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    val fill = if (index == 0) MaterialTheme.colorScheme.surfaceContainerHighest else if (dark) tint.dark else tint.light
    val progress by animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "swatch")
    val rotation by animateFloatAsState(if (selected) 45f else 0f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "rot")
    val morph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    Box(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .clip(MorphShape(morph, progress))
            .background(fill)
            .border(BorderStroke(if (selected) 0.dp else 1.dp, MaterialTheme.colorScheme.outlineVariant), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = tint.name; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(Icons.Filled.Check, null, Modifier.graphicsLayer { rotationZ = -rotation }, tint = if (index == 0) MaterialTheme.colorScheme.onSurface else noteContent(index))
            index == 0 -> Icon(Icons.Outlined.FormatColorReset, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LabelPickerSheet(
    labels: List<String>,
    notes: List<Note>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Etiketler")
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                placeholder = { Text("Yeni etiket") },
                singleLine = true,
                shape = CircleShape,
                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (newLabel.isNotBlank()) { onCreate(newLabel); newLabel = "" }
                }),
                trailingIcon = {
                    if (newLabel.isNotBlank()) IconButton(onClick = { onCreate(newLabel); newLabel = "" }) { Icon(Icons.Outlined.Check, "Ekle") }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            )
            labels.forEach { label ->
                val all = notes.isNotEmpty() && notes.all { label in it.labels }
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Label, null) },
                    trailingContent = { Checkbox(checked = all, onCheckedChange = { onToggle(label) }) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onToggle(label) }.padding(horizontal = 8.dp),
                )
            }
            if (labels.isEmpty()) {
                Text(
                    "Henüz etiket yok. Yukarıdan bir tane oluşturun.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
    }
}

@Composable
fun LabelManagerSheet(
    labels: List<String>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SheetTitle("Etiketleri düzenle")
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                placeholder = { Text("Yeni etiket oluştur") },
                singleLine = true,
                shape = CircleShape,
                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd(newLabel); newLabel = "" }),
                trailingIcon = { if (newLabel.isNotBlank()) IconButton(onClick = { onAdd(newLabel); newLabel = "" }) { Icon(Icons.Outlined.Check, "Ekle") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            )
            labels.forEach { label ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.Label, null) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { renaming = label }) { Icon(Icons.Outlined.Edit, "Yeniden adlandır") }
                            IconButton(onClick = { confirmDelete = label }) { Icon(Icons.Outlined.Delete, "Sil") }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
    renaming?.let { old ->
        var text by remember(old) { mutableStateOf(old) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Etiketi yeniden adlandır") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRename(old, text); renaming = null }) { Text("Kaydet") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Vazgeç") } },
        )
    }
    confirmDelete?.let { label ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("“$label” silinsin mi?") },
            text = { Text("Etiket tüm notlardan kaldırılır. Notların kendisi silinmez.") },
            confirmButton = { TextButton(onClick = { onDelete(label); confirmDelete = null }) { Text("Sil") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Vazgeç") } },
        )
    }
}

/** Quick reminder presets plus a custom date and time picker. */
@Composable
fun ReminderSheet(current: Long?, onSet: (Long?) -> Unit, onDismiss: () -> Unit) {
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<Long?>(null) }
    fun at(daysFromNow: Int, hour: Int): Long = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromNow)
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val now = System.currentTimeMillis()
    val laterToday = at(0, 18).takeIf { it > now + 60_000 } ?: at(0, 20).takeIf { it > now + 60_000 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetTitle("Hatırlatıcı")
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            if (laterToday != null) ReminderOption(Icons.Outlined.Today, "Bugün daha sonra", formatReminder(laterToday)) { onSet(laterToday) }
            ReminderOption(Icons.Outlined.WbSunny, "Yarın sabah", formatReminder(at(1, 8))) { onSet(at(1, 8)) }
            ReminderOption(Icons.Outlined.Bedtime, "Yarın akşam", formatReminder(at(1, 20))) { onSet(at(1, 20)) }
            ReminderOption(Icons.Outlined.CalendarMonth, "Tarih ve saat seç", null) { pickDate = true }
            if (current != null) {
                ReminderOption(Icons.Outlined.Close, "Hatırlatıcıyı kaldır", formatReminder(current)) { onSet(null) }
            }
        }
    }
    if (pickDate) {
        // The picker works in UTC days: hand it the local day as UTC midnight.
        val local = Calendar.getInstance().apply { timeInMillis = current ?: now }
        val utcDay = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
        val dateState = rememberDatePickerState(initialSelectedDateMillis = utcDay)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = { pickTime = dateState.selectedDateMillis ?: now; pickDate = false }) { Text("İleri") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Vazgeç") } },
        ) { DatePicker(dateState) }
    }
    pickTime?.let { dateUtc ->
        val cal = Calendar.getInstance().apply { timeInMillis = current ?: (now + 3_600_000) }
        val timeState = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = null },
            title = { Text("Saat") },
            text = { TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker gives UTC midnight of the chosen day; rebuild it in local time.
                    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtc }
                    val local = Calendar.getInstance().apply {
                        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), timeState.hour, timeState.minute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSet(local.timeInMillis)
                    pickTime = null
                }) { Text("Ayarla") }
            },
            dismissButton = { TextButton(onClick = { pickTime = null }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun ReminderOption(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Surface(shape = PolygonShape(MaterialShapes.Cookie4Sided), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp),
    )
}

