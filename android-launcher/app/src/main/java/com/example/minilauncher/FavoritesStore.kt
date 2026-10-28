package com.example.minilauncher

import android.content.Context

/** Pinned apps shown in the dock, in the order they were added. Keys are ComponentName.flattenToString(). */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY, "").orEmpty().split('\n').filter { it.isNotBlank() }

    /** Adds [key] if it is not pinned yet, removes it if it is. The dock holds at most [MAX_SIZE] apps. */
    fun toggle(key: String) {
        val current = load().toMutableList()
        if (key in current) {
            current.remove(key)
        } else if (current.size < MAX_SIZE) {
            current.add(key)
        }
        prefs.edit().putString(KEY, current.joinToString("\n")).apply()
    }

    /** Moves [key] one place left (delta -1) or right (delta 1) inside the dock. */
    fun move(key: String, delta: Int) {
        val current = load().toMutableList()
        val from = current.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in current.indices) return
        current.removeAt(from)
        current.add(to, key)
        prefs.edit().putString(KEY, current.joinToString("\n")).apply()
    }

    fun replaceAll(keys: List<String>) {
        prefs.edit().putString(KEY, keys.take(MAX_SIZE).joinToString("\n")).apply()
    }

    private companion object {
        const val KEY = "keys"
        const val MAX_SIZE = 5
    }
}
