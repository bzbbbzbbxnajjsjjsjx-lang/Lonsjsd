package com.example.minnotes

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteSortTest {

    private val notes = listOf(
        Note(1, "Alışveriş", "süt, ekmek", updated = 100),
        Note(2, "Toplantı", "bütçe planı", updated = 300, pinned = true),
        Note(3, "Fikirler", "uygulama taslağı", updated = 200),
    )

    @Test
    fun pinnedNotesComeFirstThenNewestFirst() {
        assertEquals(listOf(2L, 3L, 1L), notes.visible("", SortOrder.UPDATED).map { it.id })
    }

    @Test
    fun sortByTitleKeepsPinnedFirst() {
        // Pinned "Toplantı" stays on top; the rest are alphabetical: "Alışveriş" before "Fikirler".
        assertEquals(listOf(2L, 1L, 3L), notes.visible("", SortOrder.TITLE).map { it.id })
    }

    @Test
    fun searchMatchesTitleOrBodyIgnoringCase() {
        assertEquals(listOf(3L), notes.visible("Taslağı", SortOrder.UPDATED).map { it.id })
        assertEquals(listOf(1L), notes.visible("ekmek", SortOrder.UPDATED).map { it.id })
        assertEquals(emptyList<Long>(), notes.visible("yok", SortOrder.UPDATED).map { it.id })
    }
}
