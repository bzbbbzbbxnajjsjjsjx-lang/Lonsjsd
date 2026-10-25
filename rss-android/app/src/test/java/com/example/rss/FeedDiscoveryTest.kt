package com.example.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedDiscoveryTest {

    @Test
    fun recognisesFeedDocuments() {
        assertTrue(FeedDiscovery.isFeed("<?xml version=\"1.0\"?><rss version=\"2.0\"><channel></channel></rss>"))
        assertTrue(FeedDiscovery.isFeed("<feed xmlns=\"http://www.w3.org/2005/Atom\"></feed>"))
        assertFalse(FeedDiscovery.isFeed("<html><head><title>Ana sayfa</title></head></html>"))
    }

    @Test
    fun findsTheFeedLinkOfAWebsite() {
        val html = """
            <html><head>
              <link rel="stylesheet" href="/style.css">
              <link rel="alternate" type="application/rss+xml" title="Haberler" href="/rss/haberler.xml">
            </head><body></body></html>
        """.trimIndent()

        assertEquals("https://example.com/rss/haberler.xml", FeedDiscovery.findFeedUrl(html, "https://example.com/haber/"))
    }

    @Test
    fun withoutAFeedLinkThereIsNothingToFind() {
        assertNull(FeedDiscovery.findFeedUrl("<html><head></head></html>", "https://example.com"))
    }
}
