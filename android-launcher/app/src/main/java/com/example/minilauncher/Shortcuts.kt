package com.example.minilauncher

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process

/**
 * Shortcuts an app publishes (for example "Yeni not"). Reading them needs this launcher to be the
 * default home app; if it is not, the system refuses and the list is simply empty.
 */
fun loadAppShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val query = LauncherApps.ShortcutQuery()
        .setPackage(packageName)
        .setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
    return try {
        launcherApps.getShortcuts(query, Process.myUserHandle()).orEmpty().filter { it.isEnabled }
    } catch (e: RuntimeException) {
        emptyList()
    }
}
