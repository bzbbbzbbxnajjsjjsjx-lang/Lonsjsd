package com.example.rss

import com.example.rss.ui.settings.boldStarts
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingTextTest {

    @Test
    fun boldsTheFirstHalfOfEveryWord() {
        // "Merhaba" has 7 letters, so 4 are bold (0..3); "dünya" starts at 8 with 3 bold letters.
        assertEquals(listOf(0..3, 8..10), boldStarts("Merhaba dünya"))
    }
}
