@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.launcher.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.text.InputType
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.App
import com.example.launcher.LauncherState
import com.example.launcher.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private val OnWallpaperShadow = Shadow(Color.Black.copy(alpha = 0.5f), blurRadius = 12f)

/**
 * The whole launcher: a clock, one line naming the app that Enter would open, and a search
 * bar. There is no app list; typing is the only way in.
 */
@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val reset by vm.reset.collectAsStateWithLifecycle()
    var editText by remember { mutableStateOf<EditText?>(null) }

    // Each time home starts over: empty the box and bring the keyboard up.
    LaunchedEffect(reset, editText) {
        val field = editText ?: return@LaunchedEffect
        if (field.text.isNotEmpty()) field.text.clear()
        field.requestFocus()
        ViewCompat.getWindowInsetsController(field)?.show(WindowInsetsCompat.Type.ime())
    }

    // A home screen has nowhere to go back to; back only clears what was typed.
    BackHandler(enabled = state.query.isNotEmpty()) { editText?.text?.clear() }

    Box(
        Modifier
            .fillMaxSize()
            // Static scrim so white text and the bar read on any wallpaper.
            .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.3f), 0.35f to Color.Transparent, 0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f))),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            if (state.settings.showClock) Clock()
            Spacer(Modifier.weight(1f))
            if (state.query.isNotBlank()) Prediction(state, vm)
            Spacer(Modifier.height(12.dp))
            LauncherSearchBar(state, vm, onEditText = { editText = it })
            Spacer(Modifier.height(12.dp))
        }
    }

    if (state.settingsOpen) SettingsSheet(state, vm, onDismiss = { vm.openSettings(false) })
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000)
        }
    }
    Column(
        Modifier
            .padding(top = 40.dp)
            .clickable(onClickLabel = "Alarmları aç") {
                runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            .padding(8.dp),
    ) {
        Text(
            android.text.format.DateFormat.getTimeFormat(context).format(Date(now)),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp, lineHeight = 92.sp, fontWeight = FontWeight.Medium, shadow = OnWallpaperShadow),
            color = Color.White,
        )
        Text(
            DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)),
            style = MaterialTheme.typography.titleLarge.copy(shadow = OnWallpaperShadow),
            color = Color.White,
        )
    }
}

/** One line: the app Enter would open, or that nothing matches. Never a list. */
@Composable
private fun Prediction(state: LauncherState, vm: LauncherViewModel) {
    val best = state.best
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (best != null) {
            AppIcon(best, vm, 40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    best.label,
                    style = MaterialTheme.typography.titleLarge.copy(shadow = OnWallpaperShadow),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val more = state.matches.size - 1
                Text(
                    when {
                        more == 0 && state.settings.autoLaunch && state.query.trim().length >= 2 -> "Açılıyor"
                        more == 0 -> "Enter ile aç"
                        else -> "Enter ile aç · $more uygulama daha uyuyor"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = OnWallpaperShadow),
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        } else {
            Text(
                if (state.settings.webSearch) "Eşleşen uygulama yok · Enter ile web'de ara" else "Eşleşen uygulama yok",
                style = MaterialTheme.typography.bodyLarge.copy(shadow = OnWallpaperShadow),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun AppIcon(app: App, vm: LauncherViewModel, sizeDp: Int) {
    val px = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
    val epoch = vm.state.collectAsStateWithLifecycle().value.iconEpoch
    val bitmap by produceState<android.graphics.Bitmap?>(null, app.key, px, epoch) {
        value = withContext(Dispatchers.IO) { vm.source.icon(app, px) }
    }
    bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(sizeDp.dp)) }
        ?: Spacer(Modifier.size(sizeDp.dp))
}

/**
 * The Material 3 search bar. Its text field is a platform EditText so the keyboard can be
 * told what Launcher3's own search box tells it (res/layout/search_container_all_apps.xml):
 * inputType text|textNoSuggestions, flagNoExtractUi. Compose text fields cannot turn
 * suggestions off, and keyboard suggestions replacing what was typed break app matching.
 */
@Composable
private fun LauncherSearchBar(state: LauncherState, vm: LauncherViewModel, onEditText: (EditText) -> Unit) {
    val view = LocalView.current
    val colors = MaterialTheme.colorScheme
    val textColor = colors.onSurface.toArgb()
    val hintColor = colors.onSurfaceVariant.toArgb()
    SearchBar(
        state = rememberSearchBarState(),
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            Row(
                Modifier.fillMaxWidth().height(SearchBarDefaults.InputFieldHeight).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context -> searchEditText(context, vm, view).also(onEditText) },
                    update = {
                        it.setTextColor(textColor)
                        it.setHintTextColor(hintColor)
                    },
                )
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.startOver() }) { Icon(Icons.Filled.Close, "Temizle") }
                } else {
                    IconButton(onClick = { vm.openSettings(true) }) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                }
            }
        },
    )
}

private fun searchEditText(context: Context, vm: LauncherViewModel, host: View) = EditText(context).apply {
    // A recreated activity gets a new, empty field: start it from what the view model holds.
    setText(vm.state.value.query)
    setSelection(text.length)
    background = null
    setPadding(0, 0, 0, 0)
    isSingleLine = true
    hint = "Uygulama adı yazın"
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
    imeOptions = EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_EXTRACT_UI
    contentDescription = "Uygulama ara"
    doAfterTextChanged { vm.setQuery(it?.toString().orEmpty()) }
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_GO) {
            host.performHapticFeedback(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
            vm.submit()
            true
        } else false
    }
}
