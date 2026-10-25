package com.example.rss

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullTextTest {

    @Test
    fun keepsLongParagraphsAndDropsPageChrome() {
        val html = """
            <html><body>
              <nav><p>Ana sayfa menüsü bağlantısı burada</p></nav>
              <script>var x = "<p>bu bir script parçasıdır ve atılmalıdır uzun uzun</p>";</script>
              <article>
                <p>Kısa.</p>
                <p>Yeni nesil asistanlar kod yazmaktan takvim yönetimine kadar her alanda çalışıyor.</p>
                <p>İkinci uzun paragraf da yazının gövdesinin bir parçasıdır ve okunmalıdır.</p>
              </article>
              <footer><p>Telif hakkı bilgisi ve iletişim adresi burada yer alır</p></footer>
            </body></html>
        """.trimIndent()

        val text = FullText.extract(html)

        assertEquals(
            "Yeni nesil asistanlar kod yazmaktan takvim yönetimine kadar her alanda çalışıyor.\n\n" +
                "İkinci uzun paragraf da yazının gövdesinin bir parçasıdır ve okunmalıdır.",
            text,
        )
        assertFalse(text.contains("menüsü"))
        assertFalse(text.contains("Telif"))
    }

    @Test
    fun attributeCodeIsNeverShownAsText() {
        val html = """
            <article>
              <p onclick="a').click(); event.preventDefault();">Tam Boyutta Gör yaklaşık 30 yıllık geçmişe sahip açık kaynaklı bir programlama dili.</p>
              <p>Bu cümle normal bir paragraftır ve yazının gerçek metnine aittir.</p>
            </article>
        """.trimIndent()

        val text = FullText.extract(html)

        assertFalse(text.contains("click"))
        assertFalse(text.contains("preventDefault"))
        assertTrue(text.contains("Tam Boyutta"))
    }
}
