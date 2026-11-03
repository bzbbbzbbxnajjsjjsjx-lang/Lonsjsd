/*
 * The matching rules are ported from AOSP Launcher3's StringMatcherUtility and
 * DefaultAppSearchAlgorithm (packages/apps/Launcher3, src/com/android/launcher3/search/).
 * Copyright (C) The Android Open Source Project. Licensed under the Apache License 2.0.
 */
package com.example.launcher

import java.text.Collator
import java.util.Locale

/**
 * App title matching as Launcher3 does it: the query must match the start of a word in the
 * title, compared with a primary-strength collator so case and accents are ignored.
 *
 * One change for Turkish keyboards: Launcher3 uses the device-locale collator, where "I" is
 * the capital of "ı", so typing "insta" would not find "Instagram". Here every i, ı, I and İ
 * is folded to the same letter and the root collator compares the rest.
 */
class AppMatcher {
    private val collator: Collator = Collator.getInstance(Locale.ROOT).apply {
        strength = Collator.PRIMARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }

    /** Index in [target] where [query] matches the start of a word, or -1. */
    fun matchIndex(query: String, title: String): Int {
        val q = foldI(query.trim())
        val target = foldI(title)
        if (q.isEmpty() || target.length < q.length) return -1
        if (q.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN }) {
            return target.lowercase().indexOf(q.lowercase())
        }
        var thisType = Character.UNASSIGNED.toInt()
        var nextType = Character.getType(target.codePointAt(0))
        val end = target.length - q.length
        for (i in 0..end) {
            val lastType = thisType
            thisType = nextType
            nextType = if (i < target.length - 1) Character.getType(target.codePointAt(i + 1)) else Character.UNASSIGNED.toInt()
            if (isBreak(thisType, lastType, nextType) && prefixMatches(q, target.substring(i, i + q.length))) return i
        }
        return -1
    }

    fun matches(query: String, target: String) = matchIndex(query, target) >= 0

    // Same length in and out, so match positions still point into the original title.
    private fun foldI(s: String) = s.replace('ı', 'i').replace('İ', 'I')

    private fun prefixMatches(query: String, target: String): Boolean {
        val compare = collator.compare(query, target)
        if (compare == 0) return true
        // A target with a modifier can sort after the query at the same length; adding the
        // largest character to the query shows whether the query was really its prefix.
        return compare < 0 && collator.compare(query + '￿', target) >= 0
    }

    private fun isBreak(thisType: Int, prevType: Int, nextType: Int): Boolean {
        when (prevType) {
            Character.UNASSIGNED.toInt(),
            Character.SPACE_SEPARATOR.toInt(),
            Character.LINE_SEPARATOR.toInt(),
            Character.PARAGRAPH_SEPARATOR.toInt() -> return true
        }
        return when (thisType) {
            Character.UPPERCASE_LETTER.toInt() -> {
                if (nextType != Character.UPPERCASE_LETTER.toInt() && nextType != Character.OTHER_SYMBOL.toInt() &&
                    nextType != Character.DECIMAL_DIGIT_NUMBER.toInt() && nextType != Character.UNASSIGNED.toInt()
                ) true else prevType != Character.UPPERCASE_LETTER.toInt()
            }
            Character.TITLECASE_LETTER.toInt() -> prevType != Character.UPPERCASE_LETTER.toInt()
            Character.LOWERCASE_LETTER.toInt() ->
                prevType > Character.OTHER_LETTER.toInt() || prevType <= Character.UNASSIGNED.toInt()
            Character.DECIMAL_DIGIT_NUMBER.toInt(),
            Character.LETTER_NUMBER.toInt(),
            Character.OTHER_NUMBER.toInt() -> !(prevType == Character.DECIMAL_DIGIT_NUMBER.toInt() ||
                prevType == Character.LETTER_NUMBER.toInt() || prevType == Character.OTHER_NUMBER.toInt())
            Character.MATH_SYMBOL.toInt(),
            Character.CURRENCY_SYMBOL.toInt(),
            Character.OTHER_PUNCTUATION.toInt(),
            Character.DASH_PUNCTUATION.toInt() -> true
            else -> false
        }
    }

    /**
     * Apps whose title matches, best first: the whole title typed out, then a match at the
     * start of the title, then at a later word; within each, the app opened more often, then
     * alphabetical.
     */
    fun <T> rank(query: String, items: List<T>, title: (T) -> String, uses: (T) -> Int): List<T> {
        val q = foldI(query.trim())
        // Sort keys are worked out once per match, not inside the comparator.
        class Key(val item: T, val tier: Int, val uses: Int, val name: java.text.CollationKey)
        return items.mapNotNull { item ->
            val t = title(item)
            val at = matchIndex(q, t)
            if (at < 0) return@mapNotNull null
            val tier = when {
                collator.compare(q, foldI(t)) == 0 -> 0
                at == 0 -> 1
                else -> 2
            }
            Key(item, tier, uses(item), collator.getCollationKey(foldI(t)))
        }
            .sortedWith(compareBy<Key> { it.tier }.thenByDescending { it.uses }.thenBy { it.name })
            .map { it.item }
    }
}
