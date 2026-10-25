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

        val DEFAULT_FEEDS = listOf(
            Feed("hn", "Hacker News", "https://hnrss.org/frontpage"),
            Feed("verge", "The Verge", "https://www.theverge.com/rss/index.xml"),
            Feed("bbc", "BBC Teknoloji", "https://feeds.bbci.co.uk/news/technology/rss.xml"),
        )
    }
}
