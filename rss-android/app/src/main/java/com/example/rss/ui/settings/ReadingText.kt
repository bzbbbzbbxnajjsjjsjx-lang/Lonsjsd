package com.example.rss.ui.settings

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/** The first half (rounded up) of every word, as character ranges: these are the letters shown in bold. */
fun boldStarts(text: String): List<IntRange> =
    Regex("\\p{L}+").findAll(text).map { match ->
        val count = (match.value.length + 1) / 2
        match.range.first until match.range.first + count
    }.toList()

/** The article text; with easy reading on, the starts of the words are bold, so the eye moves faster. */
fun readingText(text: String, easy: Boolean): AnnotatedString {
    if (!easy) return AnnotatedString(text)
    return buildAnnotatedString {
        var position = 0
        boldStarts(text).forEach { range ->
            if (range.first > position) append(text.substring(position, range.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(range.first, range.last + 1)) }
            position = range.last + 1
        }
        if (position < text.length) append(text.substring(position))
    }
}
