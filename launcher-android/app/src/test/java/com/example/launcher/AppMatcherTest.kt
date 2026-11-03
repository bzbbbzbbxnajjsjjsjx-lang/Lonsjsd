package com.example.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppMatcherTest {
    private val m = AppMatcher()

    @Test fun matchesWordStartsOnly() {
        assertTrue(m.matches("cam", "Camera"))
        assertTrue(m.matches("maps", "Google Maps"))
        assertFalse(m.matches("amera", "Camera"))
        assertFalse(m.matches("xyz", "Camera"))
    }

    @Test fun caseAccentsAndTurkishI() {
        assertTrue(m.matches("ayarlar", "Ayarlar"))
        assertTrue(m.matches("calis", "Çalışma"))
        assertTrue(m.matches("insta", "Instagram"))
        assertTrue(m.matches("İNSTA", "Instagram"))
        assertTrue(m.matches("ılık", "Ilık Su"))
    }

    @Test fun camelCaseBreaks() {
        assertTrue(m.matches("app", "WhatsApp"))
        assertTrue(m.matches("tube", "YouTube"))
    }

    @Test fun rankingPrefersTitleStartThenUse() {
        val apps = listOf("Google Chrome", "Chrome Beta", "Chrome")
        val uses = mapOf("Chrome Beta" to 5)
        assertEquals(listOf("Chrome", "Chrome Beta", "Google Chrome"), m.rank("chrome", apps, { it }, { uses[it] ?: 0 }))
        assertEquals(listOf("Chrome Beta", "Chrome", "Google Chrome"), m.rank("chr", apps, { it }, { uses[it] ?: 0 }))
        assertEquals(emptyList<String>(), m.rank("zz", apps, { it }, { 0 }))
    }
}
