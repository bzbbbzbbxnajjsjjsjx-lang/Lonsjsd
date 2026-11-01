@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.notes.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notes.NotesViewModel
import com.example.notes.UiState
import com.example.notes.data.ThemeMode

@Composable
fun SettingsScreen(state: UiState, vm: NotesViewModel, onBack: () -> Unit) {
    val haptics = rememberHaptics()
    val s = state.settings
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    BackHandler { haptics.tick(); onBack() }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::import) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Ayarlar", fontWeight = FontWeight.Black) },
                subtitle = { Text("${state.notes.size} not · ${state.allLabels.size} etiket") },
                navigationIcon = {
                    IconButton(onClick = { haptics.tick(); onBack() }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { GroupTitle("Görünüm") }
            item {
                SettingsCard(position = Pos.First) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingIcon(Icons.Outlined.Contrast)
                            Spacer(Modifier.size(16.dp))
                            Text("Tema", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(14.dp))
                        // Connected toggle button group.
                        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
                            val modes = listOf(
                                Triple(ThemeMode.System, "Sistem", Icons.Outlined.PhoneAndroid),
                                Triple(ThemeMode.Light, "Açık", Icons.Outlined.LightMode),
                                Triple(ThemeMode.Dark, "Koyu", Icons.Outlined.DarkMode),
                            )
                            modes.forEachIndexed { i, (mode, label, icon) ->
                                val checked = s.themeMode == mode
                                ToggleButton(
                                    checked = checked,
                                    onCheckedChange = { haptics.segment(); vm.updateSettings { it.copy(themeMode = mode) } },
                                    shapes = when (i) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                ) {
                                    Icon(if (checked) Icons.Outlined.Check else icon, null, Modifier.size(ToggleButtonDefaults.IconSize))
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    SwitchRow(Icons.Outlined.AutoAwesome, "Dinamik renk", "Renkleri duvar kağıdınızdan alır", s.dynamicColor) { v ->
                        haptics.toggle(v); vm.updateSettings { it.copy(dynamicColor = v) }
                    }
                }
            }
            item {
                SwitchRow(Icons.Outlined.DarkMode, "Saf siyah", "Koyu temada OLED için tam siyah yüzeyler", s.amoled) { v ->
                    haptics.toggle(v); vm.updateSettings { it.copy(amoled = v) }
                }
            }
            item {
                SwitchRow(Icons.Outlined.GridView, "Izgara görünümü", "Notları iki sütunda gösterir", s.grid, Pos.Last) { v ->
                    haptics.toggle(v); vm.updateSettings { it.copy(grid = v) }
                }
            }

            item { GroupTitle("Yazı") }
            item {
                var local by remember(s.textScale) { mutableFloatStateOf(s.textScale) }
                SliderRow(
                    Icons.Outlined.FormatSize, "Düzenleyici yazı boyutu", "%${(local * 100).toInt()}",
                    value = local, range = 0.8f..1.5f, steps = 6, position = Pos.First,
                    onChange = { if (it != local) haptics.segment(); local = it },
                    onDone = { vm.updateSettings { it.copy(textScale = local) } },
                )
            }
            item {
                var local by remember(s.showPreviewLines) { mutableFloatStateOf(s.showPreviewLines.toFloat()) }
                SliderRow(
                    Icons.Outlined.Notes, "Önizleme satırları", "${local.toInt()} satır",
                    value = local, range = 2f..12f, steps = 9, position = Pos.Last,
                    onChange = { if (it.toInt() != local.toInt()) haptics.segment(); local = it },
                    onDone = { vm.updateSettings { it.copy(showPreviewLines = local.toInt()) } },
                )
            }

            item { GroupTitle("Geri bildirim") }
            item {
                SwitchRow(Icons.Outlined.Vibration, "Dokunsal geri bildirim", "Dokunuşlarda hafif titreşim", s.haptics, Pos.Single) { v ->
                    vm.updateSettings { it.copy(haptics = v) }
                }
            }

            item { GroupTitle("Yedekleme") }
            item {
                ActionRow(Icons.Outlined.Upload, "Dışa aktar", "Tüm notları bir JSON dosyasına kaydeder", Pos.First) {
                    haptics.tick(); exporter.launch("notlar-yedek.json")
                }
            }
            item {
                ActionRow(Icons.Outlined.Download, "İçe aktar", "Yedek dosyasından notları geri yükler", Pos.Last) {
                    haptics.tick(); importer.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }

            item { GroupTitle("Hakkında") }
            item {
                ActionRow(Icons.Outlined.Info, "Notlar 2.0", "Material 3 Expressive ile yapıldı", Pos.Single) { haptics.tick() }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

enum class Pos { First, Middle, Last, Single }

private fun shapeFor(pos: Pos) = when (pos) {
    Pos.First -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    Pos.Middle -> RoundedCornerShape(6.dp)
    Pos.Last -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    Pos.Single -> RoundedCornerShape(24.dp)
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(position: Pos, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    if (onClick != null) {
        Surface(onClick = onClick, shape = shapeFor(position), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        Surface(shape = shapeFor(position), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Surface(shape = PolygonShape(MaterialShapes.Cookie7Sided), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, position: Pos = Pos.Middle, onChange: (Boolean) -> Unit) {
    SettingsCard(position, onClick = { onChange(!checked) }) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { SettingIcon(icon) },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onChange,
                    thumbContent = if (checked) { { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) } } else null,
                )
            },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, position: Pos, onClick: () -> Unit) {
    SettingsCard(position, onClick = onClick) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { SettingIcon(icon) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

@Composable
private fun SliderRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    position: Pos,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    SettingsCard(position) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon(icon)
                Spacer(Modifier.size(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(valueText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps, onValueChangeFinished = onDone, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Suppress("unused")
private fun Modifier.roundClip() = this.clip(RoundedCornerShape(24.dp))
