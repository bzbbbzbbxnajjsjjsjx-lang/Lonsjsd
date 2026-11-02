package com.example.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private fun best(query: String, vararg labels: String) = labels.maxBy { Search.score(it, query) }

    @Test fun turkishAndAccentsMatch() {
        assertEquals("calisma agi", Search.normalize("Çalışma Ağı"))
        assertTrue(Search.score("Ayarlar", "AYARLAR") > 0)
        assertTrue(Search.score("Çalışma", "calis") > 0)
        assertTrue(Search.score("İnstagram", "insta") > 0)
    }

    @Test fun tiersRankSensibly() {
        assertEquals("Chrome", best("chrome", "Chrome", "Chrome Beta"))
        assertEquals("Haritalar", best("har", "Haritalar", "Kamera Hareketi"))
        assertTrue(Search.score("Google Maps", "gm") > 0)
        assertTrue(Search.score("Google Maps", "maps") > Search.score("Google Maps", "gma"))
        assertTrue(Search.score("WhatsApp", "wtsp") > 0)
        assertEquals(0, Search.score("Telefon", "xyz"))
        // Generic package parts must not make every app match a short query.
        assertEquals(0, Search.score("Müzik", "an", "com.android.music"))
        assertTrue(Search.score("Müzik", "spot", "com.spotify.music") > 0)
    }

    @Test fun calculator() {
        assertEquals("7", Calculator.format(Calculator.evaluate("1+2*3")!!))
        assertEquals("20", Calculator.format(Calculator.evaluate("200*10%")!!))
        assertEquals("2,5", Calculator.format(Calculator.evaluate("5/2")!!))
        assertEquals("9", Calculator.format(Calculator.evaluate("(1+2)^2")!!))
        assertEquals("3,5", Calculator.format(Calculator.evaluate("1,5 + 2")!!))
        assertEquals("-4", Calculator.format(Calculator.evaluate("-2^2")!!))
        assertNull(Calculator.evaluate("123"))
        assertNull(Calculator.evaluate("chrome"))
        assertNull(Calculator.evaluate("1/0"))
    }
}
