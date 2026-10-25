@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.example.rss.ui

import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Feed
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialShapes
import com.example.rss.Article
import com.example.rss.Feed
import com.example.rss.FeedViewModel
import com.example.rss.Screen
import com.example.rss.UiState

/** The card's resting and pressed outlines: a rounded square that morphs into a cookie. */
private val cardMorph = Morph(MaterialShapes.Square, MaterialShapes.Cookie9Sided)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    // The floating toolbar folds away while the list moves down and opens again on the way up.
    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val down = index > lastIndex || (index == lastIndex && offset > lastOffset + 4)
                val up = index < lastIndex || (index == lastIndex && offset < lastOffset - 4)
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(state.screen.title, fontWeight = FontWeight.SemiBold) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Yenile")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state.screen) {
                Screen.Sources -> SourcesList(state, vm, listState)
                else -> ArticleList(
                    state = state,
                    vm = vm,
                    listState = listState,
                    onOpen = { openArticle = it },
                )
            }

            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = {
                            if (state.screen == Screen.Sources) showAddFeed = true else vm.markAllRead()
                        },
                    ) {
                        Icon(
                            if (state.screen == Screen.Sources) Icons.Rounded.Add else Icons.Rounded.DoneAll,
                            contentDescription = if (state.screen == Screen.Sources) "Kaynak ekle" else "Hepsini okundu işaretle",
                        )
                    }
                },
            ) {
                listOf(
                    Triple(Screen.Stream, Icons.Rounded.Feed, "Akış"),
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

    if (showAddFeed) AddFeedDialog(
        onDismiss = { showAddFeed = false },
        onAdd = { url, onError -> vm.addFeed(url) { result -> result.onSuccess { showAddFeed = false }.onFailure { onError(it.message.orEmpty()) } } },
    )

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArticleList(
    state: UiState,
    vm: FeedViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onOpen: (Article) -> Unit,
) {
    val visible = state.visible
    val showHero = state.screen == Screen.Stream && visible.isNotEmpty()
    val hero = if (showHero) visible.first() else null
    val rest = if (showHero) visible.drop(1) else visible

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.screen == Screen.Stream) {
            item(key = "chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
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
                ArticleCard(
                    article = hero,
                    read = hero.id in state.read,
                    saved = state.saved.containsKey(hero.id),
                    hero = true,
                    onOpen = { onOpen(hero) },
                    onToggleSave = { vm.toggleSaved(hero) },
                )
            }
        }

        itemsIndexed(rest, key = { _, article -> article.id }) { _, article ->
            ArticleCard(
                article = article,
                read = article.id in state.read,
                saved = state.saved.containsKey(article.id),
                hero = false,
                onOpen = { onOpen(article) },
                onToggleSave = { vm.toggleSaved(article) },
                modifier = Modifier.animateContentSize(),
            )
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = when {
                        state.screen == Screen.Saved -> "Henüz kaydedilmiş bir yazı yok."
                        state.feeds.isEmpty() -> "Henüz kaynak yok. Kaynaklar sekmesinden bir RSS bağlantısı ekleyin."
                        else -> "Gösterilecek yazı yok. Yenilemeyi deneyin."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArticleCard(
    article: Article,
    read: Boolean,
    saved: Boolean,
    hero: Boolean,
    onOpen: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Pressing morphs the card's outline from a rounded square into a cookie, on the spring.
    val morph by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "card-morph",
    )
    val shape = remember(morph) { MorphShape(cardMorph, morph) }
    val textAlpha = if (read) 0.7f else 1f

    ElevatedCard(
        onClick = onOpen,
        interactionSource = interaction,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .then(if (hero) Modifier.height(260.dp) else Modifier),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${article.source} · ${relativeTime(article.dateMillis)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.title,
                style = if (hero) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                maxLines = if (hero) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                IconToggleButton(checked = saved, onCheckedChange = { onToggleSave() }) {
                    Icon(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (saved) "Kayıttan çıkar" else "Kaydet",
                    )
                }
            }
        }
    }
}

@Composable
private fun SourcesList(state: UiState, vm: FeedViewModel, listState: androidx.compose.foundation.lazy.LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.feeds, key = { it.id }) { feed ->
            SourceRow(feed = feed, onRemove = { vm.removeFeed(feed.id) })
        }
        if (state.feeds.isEmpty()) {
            item {
                Text(
                    text = "Henüz kaynak yok. Sağ alttaki düğmeyle bir RSS bağlantısı ekleyin.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SourceRow(feed: Feed, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(feed.title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(feed.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Rounded.RssFeed, contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = "${feed.title} kaynağını kaldır")
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
    )
}

@Composable
private fun AddFeedDialog(onDismiss: () -> Unit, onAdd: (String, (String) -> Unit) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaynak ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("RSS veya Atom bağlantısını yapıştırın.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = "" },
                    label = { Text("Bağlantı") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onToggleSave) {
                    Icon(
                        if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (saved) "Kaydedildi" else "Kaydet")
                }
                Button(onClick = onOpenSource) {
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
