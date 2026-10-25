@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.rss.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.automirrored.rounded.Feed
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss.Article
import com.example.rss.Feed
import com.example.rss.FeedViewModel
import com.example.rss.Screen
import com.example.rss.UiState

/** Cards use calm, consistent corners. Only the save badge morphs. */
private val HeroShape = RoundedCornerShape(32.dp)
private val CardShape = RoundedCornerShape(24.dp)

/** The save badge morphs from a rounded square into a cookie when the article is saved. */
private val saveMorph = Morph(MaterialShapes.Square, MaterialShapes.Cookie9Sided)

@Composable
fun AppScreen(vm: FeedViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }

    var toolbarExpanded by remember { mutableStateOf(true) }
    var showAddFeed by remember { mutableStateOf(false) }
    var openArticle by remember { mutableStateOf<Article?>(null) }

    // The toolbar folds while the list moves down and opens again on the way up.
    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val down = index > lastIndex || (index == lastIndex && offset > lastOffset + 6)
                val up = index < lastIndex || (index == lastIndex && offset < lastOffset - 6)
                if (down && index > 0) toolbarExpanded = false
                if (up || index == 0) toolbarExpanded = true
                lastIndex = index
                lastOffset = offset
            }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.dismissMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(state.screen.title, fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = vm::refresh, enabled = !state.loading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state.screen) {
                Screen.Sources -> SourcesList(state, vm, listState)
                else -> ArticleList(state, vm, listState, onOpen = { openArticle = it })
            }

            // Snackbars sit above the toolbar, never under it.
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp),
            )

            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                floatingActionButton = {
                    val isSources = state.screen == Screen.Sources
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = { if (isSources) showAddFeed = true else vm.markAllRead() },
                    ) {
                        Icon(
                            if (isSources) Icons.Rounded.Add else Icons.Rounded.DoneAll,
                            contentDescription = if (isSources) "Kaynak ekle" else "Hepsini okundu işaretle",
                        )
                    }
                },
            ) {
                listOf(
                    Triple(Screen.Stream, Icons.AutoMirrored.Rounded.Feed, "Akış"),
                    Triple(Screen.Saved, Icons.Rounded.Bookmark, "Kaydedilenler"),
                    Triple(Screen.Sources, Icons.Rounded.RssFeed, "Kaynaklar"),
                ).forEach { (screen, icon, label) ->
                    IconToggleButton(
                        checked = state.screen == screen,
                        onCheckedChange = { vm.select(screen) },
                    ) {
                        Icon(icon, contentDescription = label)
                    }
                }
            }
        }
    }

    if (showAddFeed) {
        AddFeedDialog(
            onDismiss = { showAddFeed = false },
            onAdd = { url, onError ->
                vm.addFeed(url) { result ->
                    result.onSuccess { showAddFeed = false }
                        .onFailure { onError(it.message.orEmpty()) }
                }
            },
        )
    }

    openArticle?.let { article ->
        LaunchedEffect(article.id) { vm.markRead(listOf(article.id)) }
        ReaderSheet(
            article = article,
            saved = state.saved.containsKey(article.id),
            onToggleSave = { vm.toggleSaved(article) },
            onOpenSource = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link))) },
            onDismiss = { openArticle = null },
        )
    }
}

@Composable
private fun ArticleList(
    state: UiState,
    vm: FeedViewModel,
    listState: LazyListState,
    onOpen: (Article) -> Unit,
) {
    val visible = state.visible
    val hero = if (state.screen == Screen.Stream) visible.firstOrNull() else null
    val rest = if (hero != null) visible.drop(1) else visible

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 144.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.screen == Screen.Stream && state.feeds.isNotEmpty()) {
            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") {
                        FilterChip(
                            selected = state.filterFeedId == null,
                            onClick = { vm.filter(null) },
                            label = { Text("Tümü") },
                        )
                    }
                    items(state.feeds, key = { it.id }) { feed ->
                        FilterChip(
                            selected = state.filterFeedId == feed.id,
                            onClick = { vm.filter(feed.id) },
                            label = { Text(feed.title) },
                        )
                    }
                }
            }
        }

        if (hero != null) {
            item(key = "hero-${hero.id}") {
                HeroCard(article = hero, read = hero.id in state.read, onOpen = { onOpen(hero) })
            }
        }

        items(rest, key = { it.id }) { article ->
            ArticleCard(
                article = article,
                read = article.id in state.read,
                saved = state.saved.containsKey(article.id),
                onOpen = { onOpen(article) },
                onToggleSave = { vm.toggleSaved(article) },
                modifier = Modifier.animateItem(),
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Inbox,
                    text = when {
                        state.screen == Screen.Saved -> "Henüz kaydedilmiş bir yazı yok.\nKartlardaki yer imine dokunun."
                        state.feeds.isEmpty() -> "Henüz kaynak yok.\nKaynaklar sekmesinden bir RSS bağlantısı ekleyin."
                        else -> "Gösterilecek yazı yok.\nYenile simgesine dokunup tekrar deneyin."
                    },
                )
            }
        }
    }
}

@Composable
private fun HeroCard(article: Article, read: Boolean, onOpen: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale by pressScale(source)

    Card(
        onClick = onOpen,
        interactionSource = source,
        shape = HeroShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .height(208.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = "${article.source} · ${relativeTime(article.dateMillis)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (read) 0.7f else 1f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (article.summary.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: Article,
    read: Boolean,
    saved: Boolean,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val scale by pressScale(source)

    Card(
        onClick = onOpen,
        interactionSource = source,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 10.dp)) {
            Text(
                text = "${article.source} · ${relativeTime(article.dateMillis)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (read) 0.6f else 1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (article.summary.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaveBadge(saved = saved, onClick = onToggleSave)
            }
        }
    }
}

/** Save toggle: its container morphs between a rounded square and a cookie on the spring. */
@Composable
private fun SaveBadge(saved: Boolean, onClick: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = if (saved) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "save-morph",
    )
    val container = lerp(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.primaryContainer,
        progress,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(MorphShape(saveMorph, progress))
            .background(container)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (saved) "Kayıttan çıkar" else "Kaydet",
            tint = if (saved) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Press feedback: the card settles to 97% on the expressive spatial spring. */
@Composable
private fun pressScale(source: MutableInteractionSource): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "press-scale",
    )
}

@Composable
private fun SourcesList(state: UiState, vm: FeedViewModel, listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 144.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.feeds, key = { it.id }) { feed ->
            SourceCard(feed = feed, onRemove = { vm.removeFeed(feed.id) })
        }
        if (state.feeds.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.RssFeed,
                    text = "Henüz kaynak yok.\nSağ alttaki + ile bir RSS bağlantısı ekleyin.",
                )
            }
        }
    }
}

@Composable
private fun SourceCard(feed: Feed, onRemove: () -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Text(
                    text = feed.title.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = feed.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = "${feed.title} kaynağını kaldır")
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 96.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddFeedDialog(onDismiss: () -> Unit, onAdd: (String, (String) -> Unit) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaynak ekle", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RSS veya Atom bağlantısını yapıştırın.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = "" },
                    label = { Text("Bağlantı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = error.isNotEmpty(),
                    supportingText = { if (error.isNotEmpty()) Text(error) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && url.isNotBlank(),
                onClick = {
                    busy = true
                    onAdd(url) { message ->
                        busy = false
                        error = message
                    }
                },
            ) { Text(if (busy) "Kontrol ediliyor…" else "Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun ReaderSheet(
    article: Article,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onOpenSource: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "${article.source} · ${relativeTime(article.dateMillis)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = article.summary.ifEmpty { "Özet yok. Tam yazı için orijinali açın." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(onClick = onToggleSave, modifier = Modifier.weight(1f)) {
                    Icon(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (saved) "Kaydedildi" else "Kaydet")
                }
                Button(onClick = onOpenSource, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Orijinali aç")
                }
            }
        }
    }
}

private fun relativeTime(millis: Long?): String {
    if (millis == null) return ""
    return DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
}

