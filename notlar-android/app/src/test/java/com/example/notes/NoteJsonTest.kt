package com.example.notes

import com.example.notes.data.CheckItem
import com.example.notes.data.Note
import com.example.notes.data.NoteJson
import com.example.notes.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteJsonTest {
    @Test fun roundTripKeepsEveryField() {
        val note = Note(
            title = "Alışveriş", isChecklist = true,
            items = listOf(CheckItem(text = "Süt", checked = true), CheckItem(text = "Ekmek")),
            color = 4, pinned = true, labels = listOf("Ev"), reminder = 1234L, trashedAt = 99L,
        )
        assertEquals(listOf(note), NoteJson.decode(NoteJson.encode(listOf(note))))
    }

    @Test fun readsFirstVersionArray() {
        val old = """[{"id":"a","title":"T","body":"B","updated":5}]"""
        val n = NoteJson.decode(old).single()
        assertEquals("T", n.title); assertEquals("B", n.body); assertEquals(5L, n.created)
    }

    @Test fun badInputThrowsNothingForEmpty() = assertTrue(NoteJson.decode("  ").isEmpty())

    @Test fun checklistToggleKeepsText() {
        val n = Note(body = "bir\n\niki").toggledChecklist()
        assertEquals(listOf("bir", "iki"), n.items.map { it.text })
        assertEquals("bir\niki", n.toggledChecklist().body)
    }

    @Test fun blankAndSearch() {
        assertTrue(Note(items = listOf(CheckItem())).isBlank)
        val n = Note(title = "Toplantı", labels = listOf("İş"))
        assertTrue(n.matches("toplantı")); assertTrue(n.matches("İş")); assertFalse(n.matches("tatil"))
    }

    @Test fun pinnedSortFirst() {
        val a = Note(title = "a", updated = 1, pinned = true)
        val b = Note(title = "b", updated = 2)
        assertEquals(listOf(a, b), listOf(b, a).sortedWith(comparator(SortOrder.Updated, pinnedFirst = true)))
        assertEquals(listOf(b, a), listOf(a, b).sortedWith(comparator(SortOrder.Updated, pinnedFirst = false)))
    }
}
