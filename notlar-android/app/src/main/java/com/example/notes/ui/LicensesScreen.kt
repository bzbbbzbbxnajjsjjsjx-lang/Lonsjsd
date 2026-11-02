@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.notes.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notes.R

private enum class License(val title: String, val textRes: Int?) {
    Apache2("Apache 2.0", R.raw.apache_2_0),
}

/** An open source component shipped inside the APK. */
private data class Library(
    val name: String,
    val license: License,
    val note: String? = null,
    val url: String,
    val copyright: String,
)

/** Everything bundled in the release APK, taken from app/build.gradle.kts and its transitive libraries. */
private val Libraries = listOf(
    Library(
        "Jetpack Compose", License.Apache2, "UI, Foundation, Animation, Runtime",
        "https://developer.android.com/jetpack/androidx/releases/compose", "Copyright The Android Open Source Project",
    ),
    Library(
        "Compose Material 3", License.Apache2, "Material 3 Expressive bileşenleri",
        "https://developer.android.com/jetpack/androidx/releases/compose-material3", "Copyright The Android Open Source Project",
    ),
    Library(
        "Material Icons Extended", License.Apache2, "simgeler",
        "https://developer.android.com/jetpack/androidx/releases/compose-material", "Copyright The Android Open Source Project",
    ),
    Library(
        "AndroidX Graphics Shapes", License.Apache2, "şekil dönüşümleri",
        "https://developer.android.com/jetpack/androidx/releases/graphics", "Copyright The Android Open Source Project",
    ),
    Library(
        "AndroidX Activity", License.Apache2, null,
        "https://developer.android.com/jetpack/androidx/releases/activity", "Copyright The Android Open Source Project",
    ),
    Library(
        "AndroidX Core ve SplashScreen", License.Apache2, null,
        "https://developer.android.com/jetpack/androidx/releases/core", "Copyright The Android Open Source Project",
    ),
    Library(
        "AndroidX Lifecycle", License.Apache2, "ViewModel ve durum toplama",
        "https://developer.android.com/jetpack/androidx/releases/lifecycle", "Copyright The Android Open Source Project",
    ),
    Library(
        "Diğer AndroidX kitaplıkları", License.Apache2, "yukarıdaki bileşenler kullanır",
        "https://developer.android.com/jetpack/androidx", "Copyright The Android Open Source Project",
    ),
    Library(
        "Kotlin standard library", License.Apache2, null,
        "https://kotlinlang.org", "Copyright JetBrains s.r.o. and Kotlin Programming Language contributors",
    ),
    Library(
        "kotlinx.coroutines", License.Apache2, null,
        "https://github.com/Kotlin/kotlinx.coroutines", "Copyright JetBrains s.r.o. and contributors",
    ),
)

private data class Credit(val title: String, val subtitle: String, val url: String?)

private val Credits = listOf(
    Credit("Material Design", "Material 3 Expressive tasarım sistemi ve renk, şekil, hareket kuralları", "https://m3.material.io"),
    Credit("Android Jetpack", "Uygulamanın üzerine kurulduğu kitaplıklar", "https://developer.android.com/jetpack"),
    Credit("Claude Code", "Uygulama Claude Code ile birlikte yazıldı", "https://claude.com/claude-code"),
)

private fun groupShape(index: Int, count: Int) = when {
    count == 1 -> RoundedCornerShape(28.dp)
    index == 0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
    index == count - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
    else -> RoundedCornerShape(6.dp)
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler { haptics.tick(); onBack() }

    fun browse(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            haptics.reject()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Lisanslar ve emeği geçenler") },
                navigationIcon = {
                    IconButton(onClick = { haptics.tick(); onBack() }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri")
                    }
                },
                scrollBehavior = scroll,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = PolygonShape(MaterialShapes.Cookie9Sided),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Gavel, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        Text(
                            "Notlar, aşağıdaki açık kaynak projelerin ve onları geliştiren toplulukların çalışmasına dayanmaktadır.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { SectionTitle("Açık kaynak") }
            itemsIndexed(Libraries, key = { _, l -> "lib-" + l.name }) { i, lib ->
                GroupRow(
                    title = lib.name,
                    subtitle = listOfNotNull(lib.license.title, lib.note).joinToString(" · "),
                    shape = groupShape(i, Libraries.size),
                ) { haptics.tick(); open = lib.name }
            }

            item { SectionTitle("Emeği geçenler") }
            itemsIndexed(Credits, key = { _, c -> "credit-" + c.title }) { i, credit ->
                GroupRow(
                    title = credit.title,
                    subtitle = credit.subtitle,
                    shape = groupShape(i, Credits.size),
                    trailing = if (credit.url != null) Icons.AutoMirrored.Outlined.OpenInNew else null,
                ) { credit.url?.let { haptics.tick(); browse(it) } }
            }
            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    Libraries.firstOrNull { it.name == open }?.let { lib ->
        LicenseSheet(lib, onOpenSite = { browse(lib.url) }, onDismiss = { open = null })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 12.dp),
    )
}

@Composable
private fun GroupRow(
    title: String,
    subtitle: String,
    shape: androidx.compose.ui.graphics.Shape,
    trailing: androidx.compose.ui.graphics.vector.ImageVector? = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) {
                Spacer(Modifier.size(12.dp))
                Icon(trailing, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LicenseSheet(lib: Library, onOpenSite: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = remember(lib.license) {
        lib.license.textRes?.let { res -> context.resources.openRawResource(res).bufferedReader().use { it.readText() } }.orEmpty()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text(lib.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(lib.copyright, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Lisans: ${lib.license.title}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onOpenSite) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Proje sayfasını aç")
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            ) {
                SelectionContainer {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
