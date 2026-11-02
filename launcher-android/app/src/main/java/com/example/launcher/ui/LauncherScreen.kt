@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.launcher.ui

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.App
import com.example.launcher.LauncherState
import com.example.launcher.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LauncherScreen(vm: LauncherViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val homeShown by vm.homeShown.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue("")) }

    // Every time home comes to the front: empty search box, and the keyboard ready to type.
    LaunchedEffect(homeShown) {
        field = TextFieldValue("")
        if (state.settings.keyboardOnHome) {
            delay(120)
            runCatching { focus.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    val scrim by animateFloatAsState(
        if (state.query.isNotEmpty() || state.drawerOpen) (state.settings.dim + 0.35f).coerceAtMost(0.85f) else state.settings.dim,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "scrim",
    )

    BackHandler(enabled = state.query.isNotEmpty() && !state.drawerOpen) { field = TextFieldValue(""); vm.clearQuery() }
    // Back on an empty home screen does nothing, like every launcher.
    BackHandler(enabled = state.query.isEmpty() && !state.drawerOpen) { keyboard?.hide() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrim))
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -120f) { haptics.tick(); keyboard?.hide(); vm.openDrawer(true) }
                    },
                ) { _, dy -> total += dy }
            },
    ) {
        // Darker at the bottom, so the search bar and dock read on any wallpaper.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.25f), 0.25f to Color.Transparent, 0.7f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f)),
            ),
        )
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            val fx = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val sp = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
            val fastFx = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            val fastSp = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
            AnimatedContent(
                targetState = state.query.isBlank(),
                transitionSpec = {
                    (fadeIn(fx) + scaleIn(sp, 0.94f)) togetherWith (fadeOut(fastFx) + scaleOut(fastSp, 0.98f))
                },
                label = "home",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { idle ->
                if (idle) HomeContent(state, vm) else ResultsPanel(state, vm)
            }
            SearchBar(
                field = field,
                onChange = { v ->
                    field = v
                    if (v.text != state.query) vm.setQuery(v.text)
                },
                onSubmit = { haptics.confirm(); vm.submit() },
                onClear = { haptics.tick(); field = TextFieldValue(""); vm.clearQuery() },
                onDrawer = { haptics.tick(); keyboard?.hide(); vm.openDrawer(true) },
                onSettings = { haptics.tick(); keyboard?.hide(); vm.openSettings(true) },
                focus = focus,
            )
        }

        AnimatedVisibility(
            visible = state.drawerOpen,
            enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it / 2 } + fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it / 2 } + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            AppDrawer(state, vm, onClose = { haptics.tick(); vm.openDrawer(false) })
        }
    }

    if (state.settingsOpen) SettingsSheet(state, vm, onDismiss = { vm.openSettings(false) })
}

@Composable
private fun HomeContent(state: LauncherState, vm: LauncherViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        if (state.settings.showClock) Clock()
        Spacer(Modifier.weight(1f))
        val dock = state.favoriteApps
        val suggestions = state.suggestions
        if (dock.isEmpty() && suggestions.isEmpty() && state.loaded) {
            Text(
                "Açmak istediğiniz uygulamanın adını yazmaya başlayın.\nYukarı kaydırınca tüm uygulamalar açılır.",
                style = MaterialTheme.typography.bodyLarge.copy(shadow = TextShadow),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 24.dp, start = 4.dp),
            )
        }
        if (suggestions.isNotEmpty()) {
            HomeLabel("Sık kullanılanlar")
            AppRow(suggestions, state, vm)
            Spacer(Modifier.height(12.dp))
        }
        if (dock.isNotEmpty()) {
            HomeLabel("Ana ekran")
            dock.chunked(4).forEach { row ->
                AppRow(row, state, vm)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private val TextShadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 12f)

@Composable
private fun HomeLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(shadow = TextShadow),
        color = Color.White.copy(alpha = 0.8f),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun AppRow(apps: List<App>, state: LauncherState, vm: LauncherViewModel) {
    Row(Modifier.fillMaxWidth()) {
        apps.forEach { app ->
            Box(Modifier.weight(1f)) { AppTile(app, state, vm, onWallpaper = true) }
        }
        repeat(4 - apps.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - value % 60_000 + 50)
        }
    }
    val locale = Locale.forLanguageTag("tr")
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    Column(
        Modifier
            .padding(top = 32.dp, start = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    haptics.tick()
                    runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
            )
            .padding(8.dp),
    ) {
        Text(
            SimpleDateFormat(if (is24) "HH:mm" else "h:mm", locale).format(now),
            style = TextStyle(fontSize = 96.sp, lineHeight = 96.sp, fontWeight = FontWeight.Black, letterSpacing = (-3).sp, shadow = TextShadow),
            color = Color.White,
        )
        Text(
            SimpleDateFormat("EEEE, d MMMM", locale).format(now),
            style = MaterialTheme.typography.titleLarge.copy(shadow = TextShadow),
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResultsPanel(state: LauncherState, vm: LauncherViewModel) {
    val haptics = rememberHaptics()
    val results = state.results.take(16)
    val calc = state.calculation
    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                reverseLayout = false,
            ) {
                if (calc != null) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "calc") {
                        ActionRow(Icons.Outlined.Calculate, "= $calc", "Hesap makinesi") {
                            haptics.confirm()
                            val clip = android.content.ClipData.newPlainText("sonuç", calc)
                            vm.getApplication<android.app.Application>().getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(clip)
                        }
                    }
                }
                if (results.isEmpty() && calc == null) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "none") {
                        Text(
                            "“${state.query}” ile eşleşen uygulama yok",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(results, key = { it.key }) { app ->
                    Box(Modifier.animateItem()) {
                        AppTile(app, state, vm, highlighted = app == results.first(), onWallpaper = false)
                    }
                }
                if (state.settings.webSearch) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "web") {
                        ActionRow(Icons.Outlined.Public, "Web'de ara: ${state.query}", null) { haptics.tick(); vm.webSearch() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = PolygonShape(MaterialShapes.Cookie4Sided), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** An app icon and its name. Tap opens it; long press shows its menu. */
@Composable
fun AppTile(app: App, state: LauncherState, vm: LauncherViewModel, highlighted: Boolean = false, onWallpaper: Boolean) {
    val haptics = rememberHaptics()
    var menu by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "press")
    val bg by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "hl",
    )
    val favorite = app.key in state.favorites
    val hidden = app.key in state.hidden
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(bg)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = androidx.compose.material3.ripple(),
                    onClick = { haptics.confirm(); vm.launch(app) },
                    onLongClick = { haptics.long(); menu = true },
                )
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                AppIcon(app, vm, 54.dp)
                if (highlighted) {
                    Surface(
                        shape = PolygonShape(MaterialShapes.Cookie6Sided),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(22.dp).graphicsLayer { translationX = 12f; translationY = 8f },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.KeyboardReturn, "Enter ile açılır", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.labelLarge.let { if (onWallpaper) it.copy(shadow = TextShadow) else it },
                color = when {
                    onWallpaper -> Color.White
                    highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            )
        }
        AppMenu(
            expanded = menu,
            app = app,
            favorite = favorite,
            hidden = hidden,
            canMoveLeft = favorite && state.favoriteApps.firstOrNull()?.key != app.key,
            canMoveRight = favorite && state.favoriteApps.lastOrNull()?.key != app.key,
            vm = vm,
            onDismiss = { menu = false },
        )
    }
}

@Composable
private fun AppMenu(
    expanded: Boolean,
    app: App,
    favorite: Boolean,
    hidden: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    DropdownMenu(expanded, onDismissRequest = onDismiss, shape = MaterialTheme.shapes.large) {
        Text(
            app.label + if (app.isWork) " (iş)" else "",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        DropdownMenuItem(
            text = { Text(if (favorite) "Ana ekrandan kaldır" else "Ana ekrana ekle") },
            leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
            onClick = { onDismiss(); vm.toggleFavorite(app) },
        )
        if (canMoveLeft) DropdownMenuItem(
            text = { Text("Sola taşı") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) },
            onClick = { onDismiss(); vm.moveFavorite(app, -1) },
        )
        if (canMoveRight) DropdownMenuItem(
            text = { Text("Sağa taşı") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            onClick = { onDismiss(); vm.moveFavorite(app, 1) },
        )
        DropdownMenuItem(
            text = { Text(if (hidden) "Çekmecede göster" else "Çekmeceden gizle") },
            leadingIcon = { Icon(if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null) },
            onClick = { onDismiss(); vm.setHidden(app, !hidden) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Uygulama bilgisi") },
            leadingIcon = { Icon(Icons.Outlined.Info, null) },
            onClick = { onDismiss(); runCatching { vm.source.openDetails(app) } },
        )
        DropdownMenuItem(
            text = { Text("Kaldır", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                runCatching {
                    // The uninstall intent can only reach this profile; a work app is removed from its info page.
                    if (app.isWork) vm.source.openDetails(app)
                    else context.startActivity(
                        Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
        )
    }
}

@Composable
fun AppIcon(app: App, vm: LauncherViewModel, size: Dp) {
    val px = with(LocalDensity.current) { size.roundToPx() }
    val epoch = vm.state.collectAsStateWithLifecycle().value.iconEpoch
    val bitmap by produceState<android.graphics.Bitmap?>(null, app.key, px, epoch) {
        value = withContext(Dispatchers.IO) { vm.source.icon(app, px) }
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size))
        } else {
            Box(Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)))
        }
        if (app.isWork && b == null) Icon(Icons.Outlined.WorkOutline, null, Modifier.size(size / 2))
    }
}

@Composable
private fun SearchBar(
    field: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onDrawer: () -> Unit,
    onSettings: () -> Unit,
    focus: FocusRequester,
) {
    val typing = field.text.isNotEmpty()
    val morph = remember { androidx.graphics.shapes.Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val progress by animateFloatAsState(if (typing) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "morph")
    val spin by animateFloatAsState(if (typing) 90f else 0f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "spin")
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).height(64.dp),
    ) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).graphicsLayer { rotationZ = spin }.clip(MorphShape(morph, progress)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, null, Modifier.graphicsLayer { rotationZ = -spin }, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (!typing) Text("Uygulama adı yazın", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                BasicTextField(
                    value = field,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Go,
                        autoCorrectEnabled = false,
                        capitalization = KeyboardCapitalization.None,
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = "Uygulama ara" },
                )
            }
            AnimatedContent(typing, label = "actions") { t ->
                if (t) {
                    IconButton(onClick = onClear, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Filled.Close, "Temizle") }
                } else {
                    Row {
                        IconButton(onClick = onDrawer, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Outlined.Apps, "Tüm uygulamalar") }
                        IconButton(onClick = onSettings, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Outlined.Settings, "Ayarlar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawer(state: LauncherState, vm: LauncherViewModel, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val apps = state.visibleApps
    val locale = Locale.forLanguageTag("tr")
    val groups = remember(apps) {
        apps.groupBy { a -> a.label.firstOrNull()?.let { c -> if (c.isLetter()) c.toString().uppercase(locale) else "#" } ?: "#" }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        modifier = Modifier.fillMaxSize().padding(top = 24.dp).statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Uygulamalar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("${apps.size} uygulama", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Outlined.ExpandMore, "Kapat") }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(84.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groups.forEach { (letter, list) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h-$letter") {
                        Text(
                            letter,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    items(list, key = { it.key }) { app -> AppTile(app, state, vm, onWallpaper = false) }
                }
            }
        }
    }
}
