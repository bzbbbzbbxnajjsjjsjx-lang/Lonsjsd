package com.example.rss

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpmlTest {

    @Test
    fun exportThenImportKeepsEveryFeed() {
        val feeds = listOf(
            Feed("a", "Hacker & News", "https://example.com/rss?x=1&y=2"),
            Feed("b", "BBC \"Teknoloji\"", "https://feeds.example.com/tech.xml"),
        )

        val parsed = Opml.parse(Opml.export(feeds))

        assertEquals(
            listOf(
                "Hacker & News" to "https://example.com/rss?x=1&y=2",
                "BBC \"Teknoloji\"" to "https://feeds.example.com/tech.xml",
            ),
            parsed,
        )
    }

    @Test
    fun ignoresOutlinesWithoutFeedAddress() {
        val xml = """<opml><body><outline text="Klasör"/><outline text="Haber" xmlUrl="https://e.com/f"/></body></opml>"""

        assertEquals(listOf("Haber" to "https://e.com/f"), Opml.parse(xml))
    }
}
