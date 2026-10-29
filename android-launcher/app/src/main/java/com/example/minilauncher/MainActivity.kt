package com.example.minilauncher

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.toShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    // Bumped every time the launcher is brought to front again (HOME pressed).
    private val homeKey = mutableIntStateOf(0)
    private val usage by lazy { UsageStore(this) }
    private val settingsStore by lazy { SettingsStore(this) }
    // Held here, not in the screen, so a theme change re-themes the whole launcher.
    private var settings by mutableStateOf(LauncherSettings())
    private val hiddenStore by lazy { HiddenStore(this) }
    private val favoritesStore by lazy { FavoritesStore(this) }

    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        uri ?: return@registerForActivityResult
        val backup = LauncherBackup(settings, favoritesStore.load(), hiddenStore.load(), usage.counts())
        contentResolver.openOutputStream(uri)?.use { it.write(BackupCodec.encode(backup).toByteArray()) }
        Toast.makeText(this, R.string.backup_done, Toast.LENGTH_SHORT).show()
    }

    private val restoreBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val text = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }.orEmpty()
            val backup = BackupCodec.decode(text)
            settingsStore.save(backup.settings)
            favoritesStore.replaceAll(backup.favorites)
            hiddenStore.replaceAll(backup.hidden)
            usage.replaceAll(backup.usage)
            recreate()
        } catch (e: Exception) {
            Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = settingsStore.load()
        Haptics.enabled = settings.haptics
        setContent {
            MiniLauncherTheme(themeMode = settings.themeMode, accent = settings.accent) {
                LauncherScreen(
                    homeKey = homeKey.intValue,
                    usage = usage,
                    settings = settings,
                    onSettingsChange = { updated ->
                        settings = updated
                        Haptics.enabled = updated.haptics
                        settingsStore.save(updated)
                    },
                    hiddenStore = hiddenStore,
                    favoritesStore = favoritesStore,
                    onLaunch = ::launchApp,
                    onAppInfo = ::openAppInfo,
                    onUninstall = ::uninstallApp,
                    onWebSearch = ::webSearch,
                    onCopy = ::copyText,
                    onShortcut = ::startShortcut,
                    onSystemPage = ::openSystemPage,
                    onBackup = { createBackup.launch("minilauncher-yedek.json") },
                    onRestore = { restoreBackup.launch(arrayOf("application/json", "text/plain")) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeKey.intValue++
    }

    /** Opens the app the way system launchers do, through LauncherApps. */
    private fun launchApp(component: ComponentName) {
        try {
            getSystemService(LauncherApps::class.java)
                .startMainActivity(component, Process.myUserHandle(), null, null)
            usage.record(component)
        } catch (e: RuntimeException) {
            // ActivityNotFoundException and SecurityException both land here.
            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun webSearch(query: String) {
        start(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
    }

    private fun copyText(text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("result", text))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun startShortcut(info: ShortcutInfo) {
        try {
            getSystemService(LauncherApps::class.java).startShortcut(info, null, null)
        } catch (e: RuntimeException) {
            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemPage(action: String) {
        start(Intent(action))
    }

    private fun openAppInfo(app: AppEntry) {
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null)))
    }

    private fun uninstallApp(app: AppEntry) {
        start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null)))
    }

    /** Starts [intent] and returns false, with a message, if no activity can handle it. */
    private fun start(intent: Intent): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (e: RuntimeException) {
            // ActivityNotFoundException and SecurityException both land here.
            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show()
            false
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(
    homeKey: Int,
    usage: UsageStore,
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    hiddenStore: HiddenStore,
    favoritesStore: FavoritesStore,
    onLaunch: (ComponentName) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onWebSearch: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onSystemPage: (String) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val appCache = remember { AppCache(context) }
    // Starts from the last saved list, so the grid is visible on the first frame.
    var apps by remember { mutableStateOf(appCache.load()) }
    var query by rememberSaveable { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }
    var usageCounts by remember { mutableStateOf(usage.counts()) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(hiddenStore.load()) }
    var favorites by remember { mutableStateOf(favoritesStore.load()) }
    var recent by remember { mutableStateOf(usage.recentKeys(3)) }
    var loading by remember { mutableStateOf(true) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Start clean and open the keyboard every time the launcher comes to front.
    LaunchedEffect(homeKey) {
        query = ""
        if (settings.autoKeyboard) {
            // Let the launcher draw its first frame before the keyboard starts animating in.
            withFrameNanos { }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // Usage changes while another app is open, so re-read it on every resume.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageCounts = usage.counts()
                recent = usage.recentKeys(3)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reload the list when apps are installed, removed or updated.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                reloadKey++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(reloadKey) {
        apps = withContext(Dispatchers.IO) {
            loadApps(context.packageManager, context.packageName).also { appCache.save(it) }
        }
        loading = false
    }

    // Recomputed on every keystroke, so results update instantly.
    val visibleApps = remember(apps, hidden, settings) {
        if (settings.showHidden) apps else apps.filter { it.component.flattenToString() !in hidden }
    }
    val results = remember(visibleApps, query, usageCounts, settings) {
        visibleApps.search(query, if (settings.sortByUsage) usageCounts else emptyMap())
    }

    val onToggleHidden: (AppEntry) -> Unit = { app ->
        Haptics.confirm(context)
        val key = app.component.flattenToString()
        if (key in hidden) hiddenStore.show(key) else hiddenStore.hide(key)
        hidden = hiddenStore.load()
    }

    val onToggleFavorite: (AppEntry) -> Unit = { app ->
        Haptics.confirm(context)
        favoritesStore.toggle(app.component.flattenToString())
        favorites = favoritesStore.load()
    }

    val onMoveFavorite: (AppEntry, Int) -> Unit = { app, delta ->
        favoritesStore.move(app.component.flattenToString(), delta)
        favorites = favoritesStore.load()
    }

    val dockApps = remember(visibleApps, favorites) {
        favorites.mapNotNull { key -> visibleApps.firstOrNull { it.component.flattenToString() == key } }
    }

    val systemMatches = remember(query) { matchingSystemShortcuts(query).take(2) }

    // Shortcuts of the app whose menu is open. Empty when this launcher is not the default home app.
    val shortcuts = remember(menuApp) {
        menuApp?.let { loadAppShortcuts(context, it.packageName) } ?: emptyList()
    }

    // A plain sum typed in the search box is shown as a result, not searched for.
    val calc = remember(query) { Calculator.evaluate(query) }

    // The system back gesture closes settings instead of leaving the launcher.
    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    // Read the expressive specs here: the transition lambda is not a composable context.
    val fadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    AnimatedContent(
        targetState = settingsOpen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(fadeSpec) + slideInVertically(slideSpec) { it / 8 }).togetherWith(fadeOut(fadeSpec))
        },
        label = "settingsTransition",
    ) { open ->
    if (open) {
        SettingsScreen(
            settings = settings,
            onChange = onSettingsChange,
            onClearUsage = {
                usage.clear()
                usageCounts = usage.counts()
            },
            onBackup = onBackup,
            onRestore = onRestore,
            onBack = { settingsOpen = false },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Title on the left, settings button in the top-right corner.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.screen_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.app_count, visibleApps.size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { settingsOpen = true }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            if (loading && apps.isEmpty()) {
                // Large shape shown only on the very first launch, before the app list is cached.
                LoadingIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 96.dp)
                        .size(120.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = results,
                    key = { _, app -> app.component.flattenToString() },
                ) { index, app ->
                    AppRow(
                        app = app,
                        shape = segmentShape(index, results.size),
                        onClick = { onLaunch(app.component) },
                        onLongClick = { menuApp = app },
                        query = query,
                    )
                }
            }

            systemMatches.forEach { shortcut ->
                ResultCard(
                    text = stringResource(shortcut.labelRes),
                    onClick = { onSystemPage(shortcut.action) },
                )
            }

            if (calc != null) {
                ResultCard(
                    text = "${query.trim()} = ${Calculator.format(calc)}",
                    onClick = {
                        Haptics.confirm(context)
                        onCopy(Calculator.format(calc))
                    },
                )
            } else if (results.isEmpty() && query.isNotBlank()) {
                Text(
                    text = stringResource(R.string.no_results),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                ResultCard(
                    text = stringResource(R.string.web_search, query.trim()),
                    onClick = { onWebSearch(query.trim()) },
                )
            }

            AnimatedVisibility(
                visible = settings.showDock && dockApps.isNotEmpty(),
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) +
                    shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
            ) {
                AppTileRow(
                    apps = dockApps,
                    onLaunch = onLaunch,
                    onLongClick = { menuApp = it },
                )
            }

            // Search sits at the bottom, within thumb reach, and is focused on open.
            SearchField(
                query = query,
                onQueryChange = { text ->
                    query = text
                    val match = if (settings.autoOpen) visibleApps.autoOpenMatch(text, usageCounts) else null
                    match?.let { app ->
                        query = ""
                        onLaunch(app.component)
                    }
                },
                onSearch = { results.firstOrNull()?.let { onLaunch(it.component) } },
                focusRequester = focusRequester,
                // The keyboard lifts only this bar. The window itself does not resize,
                // so the app list is not re-laid-out on every keyboard animation frame.
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
    }

    menuApp?.let { app ->
        val key = app.component.flattenToString()
        val items = buildList {
            add(ActionItem(Icons.Filled.Info, stringResource(R.string.app_info)) {
                menuApp = null
                onAppInfo(app)
            })
            shortcuts.forEach { info ->
                add(ActionItem(Icons.Filled.Done, info.shortLabel?.toString() ?: info.id) {
                    menuApp = null
                    onShortcut(info)
                })
            }
            add(ActionItem(Icons.Filled.Star, stringResource(if (key in favorites) R.string.menu_unfavorite else R.string.menu_favorite)) {
                menuApp = null
                onToggleFavorite(app)
            })
            if (key in favorites) {
                add(ActionItem(Icons.Filled.Refresh, stringResource(R.string.menu_move_left)) {
                    menuApp = null
                    onMoveFavorite(app, -1)
                })
                add(ActionItem(Icons.Filled.Refresh, stringResource(R.string.menu_move_right)) {
                    menuApp = null
                    onMoveFavorite(app, 1)
                })
            }
            add(ActionItem(Icons.Filled.Lock, stringResource(if (key in hidden) R.string.menu_unhide else R.string.menu_hide)) {
                menuApp = null
                onToggleHidden(app)
            })
            add(ActionItem(Icons.Filled.Delete, stringResource(R.string.uninstall), destructive = true) {
                menuApp = null
                onUninstall(app)
            })
        }
        AppActionsSheet(app = app, items = items, onDismiss = { menuApp = null })
    }
}

/**
 * Full-screen settings. Same structure as the home screen: a header row, then a
 * vertical list of segmented cards, so the two screens read as one app.
 */
@Composable
private fun SettingsScreen(
    settings: LauncherSettings,
    onChange: (LauncherSettings) -> Unit,
    onClearUsage: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SettingRow(
                icon = Icons.Filled.Home,
                label = stringResource(R.string.setting_auto_keyboard),
                checked = settings.autoKeyboard,
                shape = segmentShape(0, 10),
                onCheckedChange = { onChange(settings.copy(autoKeyboard = it)) },
            )
            SettingRow(
                icon = Icons.Filled.Done,
                label = stringResource(R.string.setting_auto_open),
                checked = settings.autoOpen,
                shape = segmentShape(1, 10),
                onCheckedChange = { onChange(settings.copy(autoOpen = it)) },
            )
            SettingRow(
                icon = Icons.Filled.Star,
                label = stringResource(R.string.setting_sort_usage),
                checked = settings.sortByUsage,
                shape = segmentShape(2, 10),
                onCheckedChange = { onChange(settings.copy(sortByUsage = it)) },
            )
            SettingRow(
                icon = Icons.Filled.Lock,
                label = stringResource(R.string.setting_show_hidden),
                checked = settings.showHidden,
                shape = segmentShape(3, 10),
                onCheckedChange = { onChange(settings.copy(showHidden = it)) },
            )
            SettingRow(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.setting_haptics),
                checked = settings.haptics,
                shape = segmentShape(4, 10),
                onCheckedChange = { onChange(settings.copy(haptics = it)) },
            )
            SettingRow(
                icon = Icons.Filled.Refresh,
                label = stringResource(R.string.setting_show_dock),
                checked = settings.showDock,
                shape = segmentShape(5, 10),
                onCheckedChange = { onChange(settings.copy(showDock = it)) },
            )
            ThemeModeCard(
                mode = settings.themeMode,
                shape = segmentShape(6, 10),
                onModeChange = { onChange(settings.copy(themeMode = it)) },
            )
            AccentCard(
                accent = settings.accent,
                shape = segmentShape(7, 10),
                onAccentChange = { onChange(settings.copy(accent = it)) },
            )
            BackupCard(
                shape = segmentShape(8, 10),
                onBackup = onBackup,
                onRestore = onRestore,
            )
            Surface(
                onClick = onClearUsage,
                modifier = Modifier.fillMaxWidth(),
                shape = segmentShape(9, 10),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(R.string.clear_usage),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = { on ->
                    Haptics.tap(context)
                    onCheckedChange(on)
                },
                // Icon colours set explicitly: the default tick was too dark to see on the thumb.
                colors = SwitchDefaults.colors(
                    checkedIconColor = MaterialTheme.colorScheme.primary,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                // Tick when on, cross when off, so the state reads without colour.
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun ThemeModeCard(mode: ThemeMode, shape: Shape, onModeChange: (ThemeMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.setting_theme_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { option ->
                    val label = when (option) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                    FilterChip(
                        selected = option == mode,
                        onClick = { onModeChange(option) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccentCard(accent: AccentColor, shape: Shape, onAccentChange: (AccentColor) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.setting_accent),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccentColor.entries.forEach { option ->
                    // The dynamic option shows the wallpaper colour; the others show their own.
                    val swatch = if (option == AccentColor.DYNAMIC) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        option.lightPrimary
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(swatch)
                            .border(
                                width = if (option == accent) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = MaterialShapes.Cookie9Sided.toShape(),
                            )
                            .clickable { onAccentChange(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupCard(shape: Shape, onBackup: () -> Unit, onRestore: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.setting_backup),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBackup) { Text(stringResource(R.string.backup_export)) }
                TextButton(onClick = onRestore) { Text(stringResource(R.string.backup_import)) }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ResultCard(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

/** Apps as a row of equal tiles, used for both the favourites dock and recent apps. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppTileRow(
    apps: List<AppEntry>,
    onLaunch: (ComponentName) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    val pill = MaterialShapes.Pill.toShape()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        apps.forEachIndexed { index, app ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.92f else 1f,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "tileScale",
            )
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { onLaunch(app.component) },
                        interactionSource = interaction,
                    ) {
                        Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        checked = false,
                        onCheckedChange = { onLongClick(app) },
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(if (index % 2 == 0) cookie else pill),
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    shape: Shape,
    query: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    // No placement animation: re-sorting by usage must not animate while the keyboard opens.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = highlightMatch(app.label, query),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
        )
    }
}

/**
 * Outer corners of the first and last row are fully rounded, inner corners are
 * tight, so a list of rows reads as one expressive grouped card.
 */
internal fun segmentShape(index: Int, count: Int): Shape {
    val outer = 28.dp
    val inner = 4.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomStart = bottom,
        bottomEnd = bottom,
    )
}
