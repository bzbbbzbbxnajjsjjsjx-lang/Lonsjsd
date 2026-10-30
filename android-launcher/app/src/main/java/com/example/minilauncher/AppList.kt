package com.example.minilauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** One launchable app. Only the label is shown; the component is used to start it. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val component: ComponentName,
)

/** Every activity with a launcher icon, minus this launcher, sorted by name. */
fun loadApps(pm: PackageManager, ownPackage: String): List<AppEntry> {
    return queryLauncherActivities(pm)
        .asSequence()
        .filter { it.activityInfo.packageName != ownPackage }
        .map { info ->
            AppEntry(
                label = info.loadLabel(pm).toString(),
                packageName = info.activityInfo.packageName,
                component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
            )
        }
        .distinctBy { it.component }
        .sortedBy { it.label.lowercase() }
        .toList()
}

/**
 * Apps matching [query] (all apps when the query is empty), most used first.
 * While searching, names that start with the query come before other matches.
 */
fun List<AppEntry>.search(query: String, usage: Map<String, Int>): List<AppEntry> {
    val q = query.trim()
    val matches = if (q.isEmpty()) this else filter { it.label.contains(q, ignoreCase = true) }
    return matches.sortedWith(
        compareBy<AppEntry>(
            { q.isNotEmpty() && !it.label.startsWith(q, ignoreCase = true) },
            { -(usage[it.component.flattenToString()] ?: 0) },
            { it.label.lowercase() },
        ),
    )
}

/**
 * The app to open right away while typing, or null. An exact name match opens at once.
 * Two or more words that leave a single match also open it, so "google maps" is enough.
 */
fun List<AppEntry>.autoOpenMatch(query: String, usage: Map<String, Int>): AppEntry? {
    val q = query.trim()
    if (q.isEmpty()) return null
    firstOrNull { it.label.equals(q, ignoreCase = true) }?.let { return it }
    if (q.split(Regex("\\s+")).size < 2) return null
    val matches = search(q, usage)
    matches.singleOrNull()?.let { return it }
    // Several apps match, but the top one starts with the full typed text: open that one.
    return matches.firstOrNull()?.takeIf { it.label.startsWith(q, ignoreCase = true) }
}

/** Counts how often each app is opened from the launcher. Stored locally on the device. */
class UsageStore(context: Context) {
    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)
    private val recentPrefs = context.getSharedPreferences("recent", Context.MODE_PRIVATE)

    fun counts(): Map<String, Int> = prefs.all.mapValues { (it.value as? Int) ?: 0 }

    fun clear() {
        prefs.edit().clear().apply()
        recentPrefs.edit().clear().apply()
    }

    fun replaceAll(counts: Map<String, Int>) {
        val edit = prefs.edit().clear()
        counts.forEach { (key, count) -> edit.putInt(key, count) }
        edit.apply()
    }

    fun record(component: ComponentName) {
        val key = component.flattenToString()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
        recentPrefs.edit().putLong(key, System.currentTimeMillis()).apply()
    }

    /** Keys of the most recently opened apps, newest first. */
    fun recentKeys(limit: Int): List<String> = recentPrefs.all
        .mapNotNull { (key, time) -> (time as? Long)?.let { key to it } }
        .sortedByDescending { it.second }
        .take(limit)
        .map { it.first }
}

/**
 * Last known app list, kept on disk so the grid can paint on the first frame
 * instead of waiting for PackageManager to answer.
 */
class AppCache(context: Context) {
    private val prefs = context.getSharedPreferences("apps_cache", Context.MODE_PRIVATE)

    fun load(): List<AppEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val item = array.getJSONObject(i)
                val pkg = item.getString("p")
                AppEntry(
                    label = item.getString("l"),
                    packageName = pkg,
                    component = ComponentName(pkg, item.getString("c")),
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun save(apps: List<AppEntry>) {
        val array = JSONArray()
        apps.forEach { app ->
            array.put(
                JSONObject()
                    .put("l", app.label)
                    .put("p", app.packageName)
                    .put("c", app.component.className),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "apps"
    }
}

private fun queryLauncherActivities(pm: PackageManager): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
}
