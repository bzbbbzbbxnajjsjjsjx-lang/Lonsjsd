@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.launcher.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings as SystemSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.launcher.LauncherState
import com.example.launcher.LauncherViewModel
import com.example.launcher.ThemeMode

@Composable
fun SettingsSheet(state: LauncherState, vm: LauncherViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val s = state.settings
    // The official way to become the home app: the HOME role on Android 10+, the system's
    // home-app picker before that.
    val roleManager = if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java) else null
    var isDefault by remember { mutableStateOf(roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true) }
    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            if (!isDefault) {
                Button(
                    onClick = {
                        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                            roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                        } else {
                            runCatching { context.startActivity(Intent(SystemSettings.ACTION_HOME_SETTINGS)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                ) { Text("Varsayılan ana ekran uygulaması yap") }
            }

            SwitchItem("Tek eşleşmede otomatik aç", "Yazdığınız ad yalnızca bir uygulamaya uyunca o uygulama açılır", s.autoLaunch) { v ->
                vm.updateSettings { it.copy(autoLaunch = v) }
            }
            SwitchItem("Web araması", "Eşleşen uygulama yoksa Enter internette arar", s.webSearch) { v ->
                vm.updateSettings { it.copy(webSearch = v) }
            }
            SwitchItem("Saat ve tarih", "Ana ekranın üstünde gösterilir", s.showClock) { v ->
                vm.updateSettings { it.copy(showClock = v) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchItem("Dinamik renk", "Renkleri duvar kağıdından alır", s.dynamicColor) { v ->
                    vm.updateSettings { it.copy(dynamicColor = v) }
                }
            }

            Text("Tema", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))
            // Connected button group, as ButtonGroupDefaults describes it.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val modes = listOf(ThemeMode.System to "Sistem", ThemeMode.Light to "Açık", ThemeMode.Dark to "Koyu")
                modes.forEachIndexed { i, (mode, label) ->
                    ToggleButton(
                        checked = s.themeMode == mode,
                        onCheckedChange = { vm.updateSettings { it.copy(themeMode = mode) } },
                        shapes = when (i) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun SwitchItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        checked = checked,
        onCheckedChange = onChange,
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    ) { Text(title) }
}

