package com.example.minilauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

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

/** Counts how often each app is opened from the launcher. Stored locally on the device. */
class UsageStore(context: Context) {
    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    fun counts(): Map<String, Int> = prefs.all.mapValues { (it.value as? Int) ?: 0 }

    fun record(component: ComponentName) {
        val key = component.flattenToString()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
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
