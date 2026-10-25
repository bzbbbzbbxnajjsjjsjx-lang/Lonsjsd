package com.example.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeedParserTest {

    private val feed = Feed("hn", "Hacker News", "https://example.com/feed")

    @Test
    fun readsRssItems() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
              <channel>
                <title>Örnek Haberler</title>
                <item>
                  <title>İlk yazı</title>
                  <link>https://example.com/1</link>
                  <description><![CDATA[<p>Kısa &amp; net <b>özet</b></p>]]></description>
                  <pubDate>Tue, 06 Oct 2026 10:00:00 +0000</pubDate>
                  <media:content url="https://example.com/1.jpg" />
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val parsed = FeedParser.parse(xml, feed)

        assertEquals("Örnek Haberler", parsed.title)
        assertEquals(1, parsed.articles.size)
        val article = parsed.articles.single()
        assertEquals("İlk yazı", article.title)
        assertEquals("https://example.com/1", article.link)
        assertEquals("Kısa & net özet", article.summary)
        assertEquals("https://example.com/1.jpg", article.image)
        assertEquals("Örnek Haberler", article.source)
        assertTrue(article.dateMillis != null)
    }

    @Test
    fun readsAtomEntriesWithHrefLinks() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom Kaynağı</title>
              <entry>
                <title>Atom yazısı</title>
                <link href="https://example.com/atom-1" />
                <summary>Özet metni</summary>
                <updated>2026-10-06T10:00:00Z</updated>
              </entry>
            </feed>
        """.trimIndent()

        val parsed = FeedParser.parse(xml, feed)

        assertEquals("Atom Kaynağı", parsed.title)
        val article = parsed.articles.single()
        assertEquals("https://example.com/atom-1", article.link)
        assertEquals("Özet metni", article.summary)
    }

    @Test
    fun skipsItemsWithoutTitleOrLink() {
        val xml = """
            <rss><channel><title>T</title>
              <item><title>Bağlantısız</title></item>
              <item><link>https://example.com/no-title</link></item>
            </channel></rss>
        """.trimIndent()

        assertTrue(FeedParser.parse(xml, feed).articles.isEmpty())
    }

    @Test
    fun invalidDateIsNull() {
        val xml = """
            <rss><channel><title>T</title>
              <item><title>Tarihsiz</title><link>https://example.com/x</link><pubDate>yarın</pubDate></item>
            </channel></rss>
        """.trimIndent()

        assertNull(FeedParser.parse(xml, feed).articles.single().dateMillis)
    }
}
