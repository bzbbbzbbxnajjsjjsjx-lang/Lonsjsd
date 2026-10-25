package com.example.rss

import android.content.Context
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.rss.ui.AppScreen
import com.example.rss.ui.RssTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h1800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val now = System.currentTimeMillis()

    /** Seeds one unreachable feed (so no network is used) and a few articles on the device. */
    private fun seed() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("rss", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = FeedStore(prefs)
        val feeds = listOf(Feed("hn", "Hacker News", "http://127.0.0.1:9/feed"))
        store.saveFeeds(feeds)
        store.saveArticles(
            listOf(
                Article("a1", "Yapay zeka araçları gündelik işleri değiştiriyor", "https://example.com/1",
                    "Yeni nesil asistanlar kod yazmaktan takvim yönetimine kadar her alanda iş akışlarını yeniden tanımlıyor.",
                    "", now - 3_600_000L, "hn", "Hacker News"),
                Article("a2", "Katı hal bataryalar seri üretime girdi", "https://example.com/2",
                    "İlk seri üretim hatları açıldı.", "", now - 7_200_000L, "hn", "Hacker News"),
                Article("a3", "Açık kaynak derleyiciler yeni sürüm çıkardı", "https://example.com/3",
                    "Performans iyileştirmeleri ve yeni hata ayıklama araçları.", "", now - 86_400_000L, "hn", "Hacker News"),
            ),
        )
        store.saveSaved(listOf(store.articles()[1]))
    }

    private fun screenshot(name: String) {
        val dir = File("build/test-screenshots").apply { mkdirs() }
        rule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun streamShowsHeroAndCards() {
        seed()
        rule.setContent { RssTheme { AppScreen() } }
        rule.waitForIdle()

        rule.onNodeWithText("Yapay zeka araçları gündelik işleri değiştiriyor").assertIsDisplayed()
        rule.onNodeWithText("Katı hal bataryalar seri üretime girdi").performScrollTo().assertIsDisplayed()
        screenshot("stream")
    }

    @Test
    fun savedScreenShowsOnlySavedArticles() {
        seed()
        rule.setContent { RssTheme { AppScreen() } }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Kaydedilenler").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Katı hal bataryalar seri üretime girdi").assertIsDisplayed()
        screenshot("saved")
    }

    @Test
    fun sourcesScreenListsFeeds() {
        seed()
        rule.setContent { RssTheme { AppScreen() } }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Kaynaklar").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Hacker News").assertIsDisplayed()
        screenshot("sources")
    }
}
