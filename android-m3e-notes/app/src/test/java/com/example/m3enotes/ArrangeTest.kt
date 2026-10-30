package com.example.m3enotes

import org.junit.Assert.assertEquals
import org.junit.Test

class ArrangeTest {

    private val notes = listOf(
        Note(1, "Alışveriş", "süt, ekmek", updated = 100),
        Note(2, "Toplantı", "bütçe planı", updated = 300, pinned = true),
        Note(3, "Fikirler", "uygulama taslağı", updated = 200),
    )

    @Test
    fun pinnedFirstThenNewestFirst() {
        assertEquals(listOf(2L, 3L, 1L), notes.arrange("", SortOrder.UPDATED).map { it.id })
    }

    @Test
    fun titleOrderKeepsPinnedFirst() {
        assertEquals(listOf(2L, 1L, 3L), notes.arrange("", SortOrder.TITLE).map { it.id })
    }

    @Test
    fun searchMatchesTitleOrBodyIgnoringCase() {
        assertEquals(listOf(3L), notes.arrange("Taslağı", SortOrder.UPDATED).map { it.id })
        assertEquals(listOf(1L), notes.arrange("EKMEK", SortOrder.UPDATED).map { it.id })
        assertEquals(emptyList<Long>(), notes.arrange("yok", SortOrder.UPDATED).map { it.id })
    }
}
