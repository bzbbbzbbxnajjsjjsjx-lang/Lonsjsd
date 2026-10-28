package com.example.minilauncher

import java.util.Locale

/** Evaluates plain arithmetic such as "12*4" or "(3+4)/2". Anything else returns null. */
object Calculator {

    fun evaluate(input: String): Double? {
        val text = input.replace(" ", "").replace(',', '.')
        // Needs at least one operator, so a bare number like "2024" is not treated as a sum.
        if (text.none { it in "+-*/" }) return null
        if (text.any { it !in "0123456789.+-*/()" }) return null
        return try {
            Parser(text).parse()
        } catch (e: IllegalArgumentException) {
            // Also covers NumberFormatException and division by zero.
            null
        }
    }

    fun format(value: Double): String {
        if (value % 1.0 == 0.0 && kotlin.math.abs(value) < 1e15) return value.toLong().toString()
        return String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
    }

    /** Recursive descent parser: expr = term (+|- term)*, term = factor (*|/ factor)*. */
    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): Double {
            val value = expr()
            require(pos == s.length) { "unexpected character" }
            return value
        }

        private fun expr(): Double {
            var value = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val right = term()
                value = if (op == '+') value + right else value - right
            }
            return value
        }

        private fun term(): Double {
            var value = factor()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val right = factor()
                require(!(op == '/' && right == 0.0)) { "division by zero" }
                value = if (op == '*') value * right else value / right
            }
            return value
        }

        private fun factor(): Double {
            require(pos < s.length) { "unexpected end" }
            if (s[pos] == '-') {
                pos++
                return -factor()
            }
            if (s[pos] == '(') {
                pos++
                val value = expr()
                require(pos < s.length && s[pos] == ')') { "missing )" }
                pos++
                return value
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start) { "expected a number" }
            return s.substring(start, pos).toDouble()
        }
    }
}
