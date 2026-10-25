package com.example.rss.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.example.rss.AppSettings
import com.example.rss.SettingsDestination

/** The page for [page]: the root lists the sub-pages, each sub-page holds its own groups. */
@Composable
fun SettingsContent(
    page: SettingsDestination,
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onOpen: (SettingsDestination) -> Unit,
    onClearReadMarks: () -> Unit,
    onExportOpml: () -> Unit,
    onImportOpml: () -> Unit,
) {
    val context = LocalContext.current
    SettingsList {
        when (page) {
            SettingsDestination.Home -> {
                group(key = "home") {
                    row("home.appearance") { shape ->
                        NavRowContent(shape, Icons.Rounded.Palette, "Görünüm", "Tema", onClick = { onOpen(SettingsDestination.Appearance) })
                    }
                    row("home.reading") { shape ->
                        NavRowContent(shape, Icons.Rounded.MenuBook, "Okuma", "Yazı boyutu, kolay okuma, sesli okuma", onClick = { onOpen(SettingsDestination.Reading) })
                    }
                    row("home.maintenance") { shape ->
                        NavRowContent(shape, Icons.Rounded.Build, "Bakım", "Okundu işaretleri ve otomatik temizlik", onClick = { onOpen(SettingsDestination.Maintenance) })
                    }
                    row("home.about") { shape ->
                        NavRowContent(shape, Icons.Rounded.Info, "Hakkında", "Sürüm bilgisi", onClick = { onOpen(SettingsDestination.About) })
                    }
                }
            }

            SettingsDestination.Appearance -> {
                group(key = "appearance.theme", header = "Tema") {
                    row("appearance.theme") { shape ->
                        ChoiceRowContent(
                            shape = shape,
                            icon = Icons.Rounded.DarkMode,
                            title = "Tema",
                            subtitle = "Sistem, açık veya koyu",
                            options = listOf("system" to "Sistem", "light" to "Açık", "dark" to "Koyu"),
                            selected = settings.themeMode,
                            onSelect = { value -> onChange { it.copy(themeMode = value) } },
                        )
                    }
                }
            }

            SettingsDestination.Reading -> {
                group(key = "reading.text", header = "Yazı") {
                    row("reading.easy") { shape ->
                        SwitchRowContent(
                            shape = shape,
                            icon = Icons.Rounded.AutoStories,
                            title = "Kolay okuma",
                            subtitle = "Satır aralığını artırır ve kelimelerin başını kalın yazar",
                            checked = settings.easyReading,
                            onChange = { value -> onChange { it.copy(easyReading = value) } },
                        )
                    }
                    row("reading.size") { shape ->
                        ChoiceRowContent(
                            shape = shape,
                            icon = Icons.Rounded.FormatSize,
                            title = "Yazı boyutu",
                            subtitle = "Yazının büyüklüğü",
                            options = listOf(1f to "Normal", 1.2f to "Büyük", 1.4f to "Çok büyük"),
                            selected = settings.textScale,
                            onSelect = { value -> onChange { it.copy(textScale = value) } },
                        )
                    }
                }
                group(key = "reading.speech", header = "Sesli okuma") {
                    row("reading.speed") { shape ->
                        ChoiceRowContent(
                            shape = shape,
                            icon = Icons.Rounded.RecordVoiceOver,
                            title = "Okuma hızı",
                            subtitle = "Yazıyı sesli okurken",
                            options = listOf(0.75f to "0,75×", 1f to "1×", 1.25f to "1,25×"),
                            selected = settings.speechRate,
                            onSelect = { value -> onChange { it.copy(speechRate = value) } },
                        )
                    }
                }
                group(key = "reading.list", header = "Akış") {
                    row("reading.hideRead") { shape ->
                        SwitchRowContent(
                            shape = shape,
                            icon = Icons.Rounded.VisibilityOff,
                            title = "Okunmuşları gizle",
                            subtitle = "Okuduğunuz yazılar akışta görünmez",
                            checked = settings.hideRead,
                            onChange = { value -> onChange { it.copy(hideRead = value) } },
                        )
                    }
                }
            }

            SettingsDestination.Maintenance -> {
                group(key = "maintenance.auto", header = "Otomatik") {
                    row("maintenance.autoRead") { shape ->
                        SwitchRowContent(
                            shape = shape,
                            icon = Icons.Rounded.DoneAll,
                            title = "7 günden eski yazıları okundu yap",
                            subtitle = "Yenilemede eski yazılar kendiliğinden kapanır",
                            checked = settings.autoReadOld,
                            onChange = { value -> onChange { it.copy(autoReadOld = value) } },
                        )
                    }
                }
                group(key = "maintenance.opml", header = "Kaynaklar") {
                    row("maintenance.exportOpml") { shape ->
                        NavRowContent(
                            shape = shape,
                            icon = Icons.Rounded.FileDownload,
                            title = "Kaynakları dışa aktar",
                            subtitle = "OPML dosyası olarak kaydedin",
                            onClick = onExportOpml,
                        )
                    }
                    row("maintenance.importOpml") { shape ->
                        NavRowContent(
                            shape = shape,
                            icon = Icons.Rounded.FileUpload,
                            title = "Kaynakları içe aktar",
                            subtitle = "OPML dosyasından kaynak ekleyin",
                            onClick = onImportOpml,
                        )
                    }
                }
                group(key = "maintenance.reset", header = "Okundu işaretleri") {
                    row("maintenance.reset") { shape ->
                        NavRowContent(
                            shape = shape,
                            icon = Icons.Rounded.Refresh,
                            title = "Okundu işaretlerini sıfırla",
                            subtitle = "Tüm yazılar yeniden okunmamış görünür",
                            onClick = onClearReadMarks,
                        )
                    }
                }
            }

            SettingsDestination.About -> {
                group(key = "about.info") {
                    row("about.version") { shape ->
                        InfoRowContent(shape, Icons.Rounded.Info, "Sürüm", versionName(context))
                    }
                }
            }
        }
    }
}

/** The reading options for the article reader, laid out like the Okuma settings page. */
@Composable
fun ReadingOptions(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("Yazı")
        SwitchRowContent(
            shape = groupShape(0, 2),
            icon = Icons.Rounded.AutoStories,
            title = "Kolay okuma",
            subtitle = "Satır aralığını artırır ve kelimelerin başını kalın yazar",
            checked = settings.easyReading,
            onChange = { value -> onChange { it.copy(easyReading = value) } },
        )
        Spacer(Modifier.height(ListItemDefaults.SegmentedGap))
        ChoiceRowContent(
            shape = groupShape(1, 2),
            icon = Icons.Rounded.FormatSize,
            title = "Yazı boyutu",
            subtitle = "Yazının büyüklüğü",
            options = listOf(1f to "Normal", 1.2f to "Büyük", 1.4f to "Çok büyük"),
            selected = settings.textScale,
            onSelect = { value -> onChange { it.copy(textScale = value) } },
        )
        SectionLabel("Sesli okuma")
        ChoiceRowContent(
            shape = groupShape(0, 1),
            icon = Icons.Rounded.RecordVoiceOver,
            title = "Okuma hızı",
            subtitle = "Yazıyı sesli okurken",
            options = listOf(0.75f to "0,75×", 1f to "1×", 1.25f to "1,25×"),
            selected = settings.speechRate,
            onSelect = { value -> onChange { it.copy(speechRate = value) } },
        )
    }
}

/** Article text style: the size choice sets the size; easy reading only opens the line spacing. */
@Composable
fun readingStyle(settings: AppSettings): TextStyle {
    val base = MaterialTheme.typography.bodyLarge
    val size = base.fontSize * settings.textScale
    return base.copy(
        fontSize = size,
        lineHeight = if (settings.easyReading) size * 1.75f else size * 1.5f,
    )
}

private fun versionName(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
