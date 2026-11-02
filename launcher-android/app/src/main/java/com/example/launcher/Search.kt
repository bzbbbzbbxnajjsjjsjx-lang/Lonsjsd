package com.example.launcher

import java.text.Normalizer
import java.util.Locale

/** Text matching for app search. Pure Kotlin so it can be unit tested. */
object Search {
    private val turkish = Locale.forLanguageTag("tr")
    private val marks = Regex("\\p{Mn}+")

    /** Lowercase, Turkish-aware, accents removed: "Çalışma Ağı" → "calisma agi". */
    fun normalize(text: String): String {
        val lower = text.lowercase(turkish).replace('ı', 'i')
        return marks.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "").trim()
    }

    private val wordSplit = Regex("[\\s._\\-:()]+|(?<=[a-z])(?=[A-Z])")

    /**
     * How well [label] matches [query]; 0 means no match. Higher tiers always win:
     * exact name, name prefix, word prefix, initials, substring, letters in order.
     */
    fun score(label: String, query: String, packageName: String = ""): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 0
        val name = normalize(label)
        val words = label.split(wordSplit).map(::normalize).filter { it.isNotEmpty() }
        val initials = words.joinToString("") { it.take(1) }
        val compact = q.replace(" ", "")
        val gaps = subsequenceGaps(name, compact)
        return when {
            name == q -> 6000
            name.startsWith(q) -> 5000 - (name.length - q.length).coerceAtMost(99)
            words.any { it.startsWith(q) } -> 4000
            compact.length >= 2 && initials.startsWith(compact) -> 3500
            name.contains(q) -> 3000
            name.replace(" ", "").contains(compact) -> 2500
            gaps != null -> 2000 - gaps.coerceAtMost(900)
            compact.length >= 3 && packageWords(packageName).any { it.startsWith(compact) } -> 500
            else -> 0
        }
    }

    private val commonPackageWords = setOf("com", "org", "net", "android", "app", "apps", "google", "mobile", "www")

    /** "com.spotify.music" → [spotify, music]; the generic parts would match everything. */
    private fun packageWords(packageName: String) =
        packageName.split('.').map(::normalize).filter { it.isNotEmpty() && it !in commonPackageWords }

    /** Letters of [q] appear in order in [text]; returns how many letters were skipped, or null. */
    private fun subsequenceGaps(text: String, q: String): Int? {
        if (q.length < 2) return null
        var i = 0
        var gaps = 0
        for (c in text) {
            if (i < q.length && c == q[i]) i++ else if (i in 1 until q.length) gaps++
        }
        return if (i == q.length) gaps else null
    }
}

/** A tiny calculator for the search box: + - * / ^, postfix %, parentheses, decimals with . or , */
object Calculator {
    private val looksLikeMath = Regex("^[0-9.,+\\-*/x×÷%^() ]+$")

    fun evaluate(input: String): Double? {
        val text = input.trim()
        if (text.isEmpty() || !looksLikeMath.matches(text)) return null
        if (text.none { it in "+-*/x×÷%^" } || text.none { it.isDigit() }) return null
        return runCatching { Parser(text.replace(',', '.').replace('x', '*').replace('×', '*').replace('÷', '/')).parse() }
            .getOrNull()?.takeIf { it.isFinite() }
    }

    fun format(value: Double): String {
        if (value == Math.rint(value) && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        return "%.10f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.').replace('.', ',')
    }

    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val v = expr()
            skip()
            require(pos == s.length)
            return v
        }

        private fun skip() { while (pos < s.length && s[pos] == ' ') pos++ }
        private fun eat(c: Char): Boolean { skip(); return if (pos < s.length && s[pos] == c) { pos++; true } else false }

        private fun expr(): Double {
            var v = term()
            while (true) v = when {
                eat('+') -> v + term()
                eat('-') -> v - term()
                else -> return v
            }
        }

        private fun term(): Double {
            var v = power()
            while (true) v = when {
                eat('*') -> v * power()
                eat('/') -> v / power()
                else -> return v
            }
        }

        // Unary minus binds looser than ^, so -2^2 is -4.
        private fun power(): Double = when {
            eat('-') -> -power()
            eat('+') -> power()
            else -> {
                val base = atom()
                if (eat('^')) Math.pow(base, power()) else base
            }
        }

        private fun atom(): Double {
            val v = if (eat('(')) {
                expr().also { require(eat(')')) }
            } else {
                skip()
                val start = pos
                while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
                require(pos > start)
                s.substring(start, pos).toDouble()
            }
            // Postfix percent: "200*10%" is 20.
            return if (eat('%')) v / 100.0 else v
        }
    }
}
