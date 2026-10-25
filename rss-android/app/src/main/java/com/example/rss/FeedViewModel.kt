package com.example.rss

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class Screen(val title: String) { Stream("Akış"), Saved("Kaydedilenler"), Sources("Kaynaklar"), Settings("Ayarlar") }

data class UiState(
    val screen: Screen = Screen.Stream,
    val feeds: List<Feed> = emptyList(),
    val articles: List<Article> = emptyList(),
    val saved: Map<String, Article> = emptyMap(),
    val read: Set<String> = emptySet(),
    val filterFeedId: String? = null,
    val loading: Boolean = false,
    val message: String? = null,
    /** Full article text, by article id, once the page has been read. */
    val fullText: Map<String, String> = emptyMap(),
    val loadingTextId: String? = null,
    val settings: AppSettings = AppSettings(),
    val settingsPage: SettingsDestination = SettingsDestination.Home,
) {
    /** Articles the current screen shows, newest first. */
    val visible: List<Article>
        get() = when (screen) {
            Screen.Saved -> saved.values.sortedByDescending { it.dateMillis ?: 0L }
            Screen.Settings -> emptyList()
            else -> {
                val ids = feeds.map { it.id }.toSet()
                articles.filter {
                    it.feedId in ids &&
                        (filterFeedId == null || it.feedId == filterFeedId) &&
                        !(settings.hideRead && it.id in read)
                }
            }
        }
}

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val store = FeedStore(
        application.getSharedPreferences("rss", Context.MODE_PRIVATE),
    )

    private val _state = MutableStateFlow(
        UiState(
            feeds = store.feeds(),
            articles = store.articles(),
            saved = store.saved().associateBy { it.id },
            read = store.read(),
            settings = store.settings(),
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun select(screen: Screen) = _state.update {
        it.copy(
            screen = screen,
            settingsPage = if (screen == Screen.Settings) SettingsDestination.Home else it.settingsPage,
        )
    }

    fun openSettingsPage(page: SettingsDestination) = _state.update { it.copy(settingsPage = page) }

    /** Goes up one settings page, or leaves Settings from its root. */
    fun settingsBack() = _state.update {
        val parent = it.settingsPage.parent
        if (parent != null) it.copy(settingsPage = parent) else it.copy(screen = Screen.Stream)
    }

    fun exportOpml(): String = Opml.export(_state.value.feeds)

    /** Adds every feed of an OPML file that is not already subscribed, then refreshes. */
    fun importOpml(xml: String) {
        val known = _state.value.feeds.map { it.url }.toMutableSet()
        val added = Opml.parse(xml)
            .filter { (_, url) -> known.add(url) }
            .map { (title, url) -> Feed(UUID.randomUUID().toString(), title, url) }
        if (added.isEmpty()) {
            _state.update { it.copy(message = "Eklenecek yeni kaynak yok") }
            return
        }
        val feeds = _state.value.feeds + added
        store.saveFeeds(feeds)
        _state.update { it.copy(feeds = feeds, message = "${added.size} kaynak eklendi") }
        refresh()
    }

    fun clearReadMarks() {
        store.saveRead(emptySet())
        _state.update { it.copy(read = emptySet(), message = "Okundu işaretleri sıfırlandı") }
    }

    fun filter(feedId: String?) = _state.update { it.copy(filterFeedId = feedId) }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun updateSettings(change: (AppSettings) -> AppSettings) {
        val next = change(_state.value.settings)
        store.saveSettings(next)
        _state.update { it.copy(settings = next) }
    }

    /** Reads the whole article page once and keeps the text for the reader. */
    fun loadFullText(article: Article) {
        val current = _state.value
        if (current.fullText.containsKey(article.id) || current.loadingTextId == article.id) return
        _state.update { it.copy(loadingTextId = article.id) }
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { FullText.extract(FeedClient.fetch(article.link)) }.getOrNull()
            }
            _state.update {
                it.copy(
                    loadingTextId = null,
                    fullText = if (text.isNullOrBlank()) it.fullText else it.fullText + (article.id to text),
                    message = if (text.isNullOrBlank()) "Tam metin alınamadı" else it.message,
                )
            }
        }
    }

    fun refresh() {
        val feeds = _state.value.feeds
        if (feeds.isEmpty() || _state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                coroutineScope {
                    feeds.map { feed ->
                        async {
                            runCatching { FeedParser.parse(FeedClient.fetch(feed.url), feed) }
                        }
                    }.awaitAll()
                }
            }

            val fresh = mutableListOf<Article>()
            val renamed = _state.value.feeds.toMutableList()
            val failed = mutableListOf<String>()
            results.forEachIndexed { i, result ->
                result.onSuccess { parsed ->
                    fresh += parsed.articles
                    renamed[i] = feeds[i].copy(title = parsed.title)
                }.onFailure { failed += feeds[i].title }
            }

            val merged = mergeArticles(fresh)
            val articles = if (fresh.isNotEmpty()) merged else _state.value.articles
            if (_state.value.settings.autoReadOld) {
                val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
                val old = articles.filter { (it.dateMillis ?: 0L) < weekAgo }.map { it.id }
                if (old.isNotEmpty()) markRead(old)
            }
            store.saveFeeds(renamed)
            if (fresh.isNotEmpty()) store.saveArticles(articles)

            _state.update {
                it.copy(
                    feeds = renamed,
                    articles = articles,
                    loading = false,
                    message = failed.takeIf { f -> f.isNotEmpty() }?.let { f -> "Yüklenemedi: ${f.joinToString()}" },
                )
            }
        }
    }

    fun addFeed(raw: String, onResult: (Result<Feed>) -> Unit) {
        val url = normalize(raw)
        if (url == null) {
            onResult(Result.failure(IllegalArgumentException("Geçerli bir bağlantı girin.")))
            return
        }
        if (_state.value.feeds.any { it.url == url }) {
            onResult(Result.failure(IllegalStateException("Bu kaynak zaten ekli.")))
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    // A website is turned into its feed: if the address is a page, its feed link is used.
                    val body = FeedClient.fetch(url)
                    val feedUrl = if (FeedDiscovery.isFeed(body)) {
                        url
                    } else {
                        FeedDiscovery.findFeedUrl(body, url) ?: error("Bu sayfada RSS veya Atom bağlantısı bulunamadı.")
                    }
                    val feedBody = if (feedUrl == url) body else FeedClient.fetch(feedUrl)
                    val draft = Feed(UUID.randomUUID().toString(), feedUrl, feedUrl)
                    val parsed = FeedParser.parse(feedBody, draft)
                    draft.copy(title = parsed.title) to parsed.articles
                }
            }
            result.onSuccess { (feed, articles) ->
                val feeds = _state.value.feeds + feed
                val merged = mergeArticles(articles + _state.value.articles)
                store.saveFeeds(feeds)
                store.saveArticles(merged)
                _state.update { it.copy(feeds = feeds, articles = merged, message = "${feed.title} eklendi") }
                onResult(Result.success(feed))
            }.onFailure {
                onResult(Result.failure(IllegalStateException(it.message ?: "Bu bağlantı okunamadı. RSS veya Atom olduğundan emin olun.")))
            }
        }
    }

    fun removeFeed(id: String) {
        val feeds = _state.value.feeds.filterNot { it.id == id }
        store.saveFeeds(feeds)
        _state.update {
            it.copy(
                feeds = feeds,
                filterFeedId = if (it.filterFeedId == id) null else it.filterFeedId,
                message = "Kaynak kaldırıldı",
            )
        }
    }

    fun toggleSaved(article: Article) {
        val saved = _state.value.saved.toMutableMap()
        val message = if (saved.remove(article.id) == null) {
            saved[article.id] = article
            "Kaydedildi"
        } else {
            "Kayıttan çıkarıldı"
        }
        store.saveSaved(saved.values.toList())
        _state.update { it.copy(saved = saved, message = message) }
    }

    fun markRead(ids: Collection<String>) {
        val read = _state.value.read + ids
        store.saveRead(read)
        _state.update { it.copy(read = read) }
    }

    fun markAllRead() {
        val ids = _state.value.visible.map { it.id }
        if (ids.isEmpty()) {
            _state.update { it.copy(message = "İşaretlenecek yazı yok") }
            return
        }
        markRead(ids)
        _state.update { it.copy(message = "Görünen yazılar okundu olarak işaretlendi") }
    }

    private fun mergeArticles(list: List<Article>): List<Article> =
        list.distinctBy { it.id }
            .sortedByDescending { it.dateMillis ?: 0L }
            .take(150)

    private fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) trimmed else "https://$trimmed"
        return runCatching { java.net.URL(withScheme).toString() }.getOrNull()
    }
}
