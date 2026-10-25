package com.example.rss

import android.util.Xml
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class Feed(val id: String, val title: String, val url: String)

data class Article(
    val id: String,
    val title: String,
    val link: String,
    val summary: String,
    val image: String,
    val dateMillis: Long?,
    val feedId: String,
    val source: String,
)

data class ParsedFeed(val title: String, val articles: List<Article>)

/** User preferences, kept on the device. */
data class AppSettings(
    val themeMode: String = "system",
    val hideRead: Boolean = false,
    val autoReadOld: Boolean = true,
    val speechRate: Float = 1f,
    /** Scale for article text: 1, 1.2 or 1.4. */
    val textScale: Float = 1f,
    /** Easy reading: wider line spacing and letter spacing for the article text. */
    val easyReading: Boolean = false,
)

/** Reads RSS (`item`) and Atom (`entry`) documents. */
object FeedParser {
    private val rfc1123 = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    private val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(xml: String, feed: Feed): ParsedFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var channelTitle: String? = null
        val articles = mutableListOf<Article>()

        var inEntry = false
        var current = mutableMapOf<String, String>()
        var image = ""
        var path = ArrayDeque<String>()
        var text = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    path.addLast(name)
                    text = StringBuilder()
                    if (name == "item" || name == "entry") {
                        inEntry = true
                        current = mutableMapOf()
                        image = ""
                    }
                    if (inEntry) {
                        when (name) {
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrBlank()) current.putIfAbsent("link", href)
                            }
                            "enclosure" -> {
                                val type = parser.getAttributeValue(null, "type").orEmpty()
                                if (type.startsWith("image")) image = parser.getAttributeValue(null, "url").orEmpty()
                            }
                            "media:content" -> {
                                if (image.isEmpty()) image = parser.getAttributeValue(null, "url").orEmpty()
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    val value = text.toString().trim()
                    if (name == "item" || name == "entry") {
                        inEntry = false
                        val title = current["title"].orEmpty()
                        val link = current["link"].orEmpty()
                        if (title.isNotEmpty() && link.isNotEmpty()) {
                            val date = current["date"]?.let(::parseDate)
                            articles += Article(
                                id = link,
                                title = title,
                                link = link,
                                summary = stripHtml(current["summary"].orEmpty()).take(320),
                                image = image,
                                dateMillis = date,
                                feedId = feed.id,
                                source = channelTitle ?: feed.title,
                            )
                        }
                    } else if (inEntry) {
                        when (name) {
                            "title" -> current.putIfAbsent("title", value)
                            "link" -> if (value.isNotEmpty()) current.putIfAbsent("link", value)
                            "description", "summary", "content:encoded", "content" ->
                                if (value.isNotEmpty()) current.putIfAbsent("summary", value)
                            "pubDate", "published", "updated", "dc:date" ->
                                if (value.isNotEmpty()) current.putIfAbsent("date", value)
                        }
                    } else if (name == "title" && channelTitle == null && path.size <= 3 && value.isNotEmpty()) {
                        channelTitle = value
                    }
                    path.removeLastOrNull()
                }
            }
            event = parser.next()
        }

        return ParsedFeed(
            title = channelTitle ?: feed.title,
            articles = articles.take(30),
        )
    }

    private fun parseDate(raw: String): Long? = runCatching { rfc1123.parse(raw)?.time }.getOrNull()
        ?: runCatching { iso8601.parse(raw.take(19))?.time }.getOrNull()

    /** Removes markup from a summary, keeping the readable text. */
    fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&lt;", "<").replace("&gt;", ">")
            .replace(Regex("\\s+"), " ")
            .trim()
}

/** Fetches a feed over HTTP. */
object FeedClient {
    fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "AkisRSS/1.0")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Keeps feeds, articles, saved articles and read marks on the device. */
class FeedStore(private val prefs: android.content.SharedPreferences) {

    fun feeds(): List<Feed> = prefs.getString(KEY_FEEDS, null)?.let { json ->
        val array = JSONArray(json)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Feed(o.getString("id"), o.getString("title"), o.getString("url"))
        }
    } ?: DEFAULT_FEEDS

    fun saveFeeds(feeds: List<Feed>) = prefs.edit().putString(KEY_FEEDS, JSONArray(feeds.map {
        JSONObject().put("id", it.id).put("title", it.title).put("url", it.url)
    }).toString()).apply()

    fun articles(): List<Article> = prefs.getString(KEY_ITEMS, null)?.let { json ->
        val array = JSONArray(json)
        List(array.length()) { i -> array.getJSONObject(i).toArticle() }
    } ?: emptyList()

    fun saveArticles(articles: List<Article>) = prefs.edit().putString(KEY_ITEMS, JSONArray(articles.map { it.toJson() }).toString()).apply()

    fun saved(): List<Article> = prefs.getString(KEY_SAVED, null)?.let { json ->
        val array = JSONArray(json)
        List(array.length()) { i -> array.getJSONObject(i).toArticle() }
    } ?: emptyList()

    fun saveSaved(articles: List<Article>) = prefs.edit().putString(KEY_SAVED, JSONArray(articles.map { it.toJson() }).toString()).apply()

    fun settings(): AppSettings = AppSettings(
        themeMode = prefs.getString(KEY_THEME, "system") ?: "system",
        hideRead = prefs.getBoolean(KEY_HIDE_READ, false),
        autoReadOld = prefs.getBoolean(KEY_AUTO_READ_OLD, true),
        speechRate = prefs.getFloat(KEY_SPEECH_RATE, 1f),
        textScale = prefs.getFloat(KEY_TEXT_SCALE, 1f),
        easyReading = prefs.getBoolean(KEY_EASY_READING, false),
    )

    fun saveSettings(settings: AppSettings) = prefs.edit()
        .putString(KEY_THEME, settings.themeMode)
        .putBoolean(KEY_HIDE_READ, settings.hideRead)
        .putBoolean(KEY_AUTO_READ_OLD, settings.autoReadOld)
        .putFloat(KEY_SPEECH_RATE, settings.speechRate)
        .putFloat(KEY_TEXT_SCALE, settings.textScale)
        .putBoolean(KEY_EASY_READING, settings.easyReading)
        .apply()

    fun read(): Set<String> = prefs.getStringSet(KEY_READ, emptySet()).orEmpty()

    fun saveRead(ids: Set<String>) = prefs.edit().putStringSet(KEY_READ, ids.toList().takeLast(500).toSet()).apply()

    private fun Article.toJson() = JSONObject()
        .put("id", id).put("title", title).put("link", link).put("summary", summary)
        .put("image", image).put("date", dateMillis ?: JSONObject.NULL)
        .put("feedId", feedId).put("source", source)

    private fun JSONObject.toArticle() = Article(
        id = getString("id"),
        title = getString("title"),
        link = getString("link"),
        summary = optString("summary"),
        image = optString("image"),
        dateMillis = if (isNull("date")) null else getLong("date"),
        feedId = getString("feedId"),
        source = getString("source"),
    )

    companion object {
        private const val KEY_FEEDS = "feeds"
        private const val KEY_ITEMS = "items"
        private const val KEY_SAVED = "saved"
        private const val KEY_READ = "read"
        private const val KEY_THEME = "theme"
        private const val KEY_HIDE_READ = "hide_read"
        private const val KEY_AUTO_READ_OLD = "auto_read_old"
        private const val KEY_SPEECH_RATE = "speech_rate"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_EASY_READING = "easy_reading"

        val DEFAULT_FEEDS = listOf(
            Feed("hn", "Hacker News", "https://hnrss.org/frontpage"),
            Feed("verge", "The Verge", "https://www.theverge.com/rss/index.xml"),
            Feed("bbc", "BBC Teknoloji", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
        )
    }
}

/** Pulls the readable text out of an article page: the paragraphs of the body, without page chrome. */
object FullText {
    private val chrome = Regex("(?is)<(script|style|nav|header|footer|aside|form|svg|noscript)[^>]*>.*?</\\1>")
    private val paragraph = Regex("(?is)<p[^>]*>(.*?)</p>")

    fun extract(html: String): String {
        val body = chrome.replace(html, " ")
        return paragraph.findAll(body)
            .map { FeedParser.stripHtml(it.groupValues[1]) }
            .filter { it.length >= 40 }
            .joinToString("\n\n")
    }
}

/**
 * Turns a website into its feed. When the address is a web page rather than an RSS or Atom
 * document, the page's own feed link (`<link rel="alternate" type="application/rss+xml">`) is used.
 */
object FeedDiscovery {
    private val feedLink = Regex("""<link\b[^>]*type\s*=\s*["']application/(?:rss|atom)\+xml["'][^>]*>""", RegexOption.IGNORE_CASE)
    private val href = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /** Whether [body] is already a feed document. */
    fun isFeed(body: String): Boolean {
        val head = body.take(4000).lowercase()
        return head.contains("<rss") || head.contains("<feed") || head.contains("<channel")
    }

    /** The feed address a page points to, resolved against the page's [base] address. */
    fun findFeedUrl(html: String, base: String): String? {
        val link = feedLink.find(html)?.value ?: return null
        val target = href.find(link)?.groupValues?.get(1) ?: return null
        return runCatching { java.net.URI(base).resolve(target).toString() }.getOrNull()
    }
}
