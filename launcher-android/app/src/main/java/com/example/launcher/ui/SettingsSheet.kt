@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.launcher.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.launcher.LauncherState
import com.example.launcher.LauncherViewModel
import com.example.launcher.Settings
import com.example.launcher.ThemeMode

@Composable
fun SettingsSheet(state: LauncherState, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val s = state.settings
    fun set(on: Boolean, f: (Settings) -> Settings) { haptics.toggle(on); vm.updateSettings(f) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("Başlatıcı", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))

            // Becoming the default home app is a system setting the user confirms.
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Home, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Ana tuşa basınca bu ekranın açılması için varsayılan başlatıcı yapın.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            haptics.confirm()
                            runCatching { context.startActivity(Intent(SystemSettings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                .onFailure { runCatching { context.startActivity(Intent(SystemSettings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                        },
                        shapes = ButtonDefaults.shapes(),
                    ) { Text("Ayarla") }
                }
            }

            Group("Arama")
            SwitchRow(Icons.Outlined.Bolt, "Tek eşleşmede otomatik aç", "Yazdığınız ad tek bir uygulamaya uyunca hemen açılır", s.autoLaunch, Pos.First) {
                set(it) { x -> x.copy(autoLaunch = it) }
            }
            SwitchRow(Icons.Outlined.Keyboard, "Klavyeyi hemen aç", "Ana ekrana dönünce yazmaya hazır olur", s.keyboardOnHome) {
                set(it) { x -> x.copy(keyboardOnHome = it) }
            }
            SwitchRow(Icons.Outlined.Public, "Web araması", "Eşleşme yoksa Enter internette arar", s.webSearch, Pos.Last) {
                set(it) { x -> x.copy(webSearch = it) }
            }

            Group("Görünüm")
            Surface(shape = shapeFor(Pos.First), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Outlined.DarkMode)
                        Spacer(Modifier.size(16.dp))
                        Text("Tema", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
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
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(if (checked) Icons.Outlined.Check else icon, null, Modifier.size(ToggleButtonDefaults.IconSize))
                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(Icons.Outlined.AutoAwesome, "Dinamik renk", "Renkleri duvar kağıdından alır", s.dynamicColor) {
                    set(it) { x -> x.copy(dynamicColor = it) }
                }
            }
            SwitchRow(Icons.Outlined.AccessTime, "Saat ve tarih", "Ana ekranın üstünde büyük saat", s.showClock) {
                set(it) { x -> x.copy(showClock = it) }
            }
            Surface(shape = shapeFor(Pos.Last), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                var dim by remember(s.dim) { mutableFloatStateOf(s.dim) }
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RowIcon(Icons.Outlined.Brightness6)
                        Spacer(Modifier.size(16.dp))
                        Text("Duvar kağıdını karart", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("%${(dim * 100).toInt()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = dim,
                        onValueChange = { if ((it * 10).toInt() != (dim * 10).toInt()) haptics.segment(); dim = it },
                        valueRange = 0f..0.8f,
                        onValueChangeFinished = { vm.updateSettings { it.copy(dim = dim) } },
                    )
                }
            }

            Group("Geri bildirim")
            SwitchRow(Icons.Outlined.Vibration, "Dokunsal geri bildirim", "Dokunuşlarda hafif titreşim", s.haptics, Pos.Single) {
                vm.updateSettings { x -> x.copy(haptics = it) }
            }

            val hidden = state.apps.filter { it.key in state.hidden }
            if (hidden.isNotEmpty()) {
                Group("Gizli uygulamalar")
                hidden.forEachIndexed { i, app ->
                    val pos = when {
                        hidden.size == 1 -> Pos.Single
                        i == 0 -> Pos.First
                        i == hidden.lastIndex -> Pos.Last
                        else -> Pos.Middle
                    }
                    Surface(shape = shapeFor(pos), color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(app, vm, 36.dp)
                            Spacer(Modifier.size(16.dp))
                            Text(app.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { haptics.tick(); vm.setHidden(app, false) }) {
                                Icon(Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Göster")
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class Pos { First, Middle, Last, Single }

fun shapeFor(pos: Pos) = when (pos) {
    Pos.First -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    Pos.Middle -> RoundedCornerShape(6.dp)
    Pos.Last -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    Pos.Single -> RoundedCornerShape(24.dp)
}

@Composable
private fun Group(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Surface(shape = PolygonShape(MaterialShapes.Cookie7Sided), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, pos: Pos = Pos.Middle, onChange: (Boolean) -> Unit) {
    Surface(
        onClick = { onChange(!checked) },
        shape = shapeFor(pos),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.size(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                thumbContent = if (checked) { { Icon(Icons.Outlined.Check, null, Modifier.size(SwitchDefaults.IconSize)) } } else null,
            )
        }
    }
}
