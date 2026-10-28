package com.example.minilauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** One launchable app. Only the label is shown; the component is used to start it. */
data class AppEntry(
    val label: String,
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
                component = ComponentName(info.activityInfo.packageName, info.activityInfo.name),
            )
        }
        .distinctBy { it.component }
        .sortedBy { it.label.lowercase() }
        .toList()
}

/**
 * Apps whose label contains [query], ignoring case. Names that start with the query
 * come first, so typing "gm" puts "Gmail" above "Google Maps".
 */
fun List<AppEntry>.search(query: String): List<AppEntry> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { it.label.contains(q, ignoreCase = true) }
        .sortedWith(
            compareBy<AppEntry>(
                { !it.label.startsWith(q, ignoreCase = true) },
                { it.label.lowercase() },
            ),
        )
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
