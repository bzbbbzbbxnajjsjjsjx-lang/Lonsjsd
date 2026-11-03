package com.example.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap

/** One launchable activity, from any user profile on the device. */
data class App(val label: String, val component: ComponentName, val user: UserHandle) {
    val key: String = component.flattenToString() + "#" + user.hashCode()
}

/** Installed apps through LauncherApps, the API a home app uses for this. */
class AppSource(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val icons = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    fun load(): List<App> = launcherApps.profiles
        // A paused work profile's apps cannot be started, so they are left out.
        .filter { user -> user == Process.myUserHandle() || !userManager.isQuietModeEnabled(user) }
        .flatMap { user -> launcherApps.getActivityList(null, user).map { App(it.label.toString(), it.componentName, user) } }
        .filterNot { it.component.packageName == context.packageName }

    /** An icon already loaded, without any work; lets the UI draw it in the very first frame. */
    fun cachedIcon(app: App, sizePx: Int): Bitmap? = icons["${app.key}@$sizePx"]

    fun icon(app: App, sizePx: Int): Bitmap? = icons["${app.key}@$sizePx"] ?: runCatching {
        launcherApps.getActivityList(app.component.packageName, app.user)
            .firstOrNull { it.componentName == app.component }
            ?.getBadgedIcon(context.resources.displayMetrics.densityDpi)
            ?.toBitmap(sizePx, sizePx)
    }.getOrNull()?.also { icons["${app.key}@$sizePx"] = it }

    /** Drops cached icons, for when an installed app may have changed. */
    fun clearIcons() = icons.clear()

    /** Starts the app; [bounds] and [options] give the system the icon to grow the window from. */
    fun launch(app: App, bounds: android.graphics.Rect?, options: android.os.Bundle?) =
        launcherApps.startMainActivity(app.component, app.user, bounds, options)

    /** Calls [onChange] on the main thread whenever installed apps change. */
    fun watch(onChange: () -> Unit): LauncherApps.Callback {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange()
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange()
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange()
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = onChange()
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        return callback
    }

    fun unwatch(callback: LauncherApps.Callback) = launcherApps.unregisterCallback(callback)
}

enum class ThemeMode { System, Light, Dark }

data class Settings(
    /** Open the app as soon as what is typed matches exactly one app. */
    val autoLaunch: Boolean = true,
    /** Enter with no matching app searches the web. */
    val webSearch: Boolean = true,
    val showClock: Boolean = true,
    val dynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
)

/** Settings and how often each app was opened, in SharedPreferences. */
class LauncherStore(context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    fun settings() = Settings(
        autoLaunch = prefs.getBoolean("autoLaunch", true),
        webSearch = prefs.getBoolean("webSearch", true),
        showClock = prefs.getBoolean("showClock", true),
        dynamicColor = prefs.getBoolean("dynamic", true),
        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme", null) } ?: ThemeMode.System,
    )

    fun save(s: Settings) = prefs.edit()
        .putBoolean("autoLaunch", s.autoLaunch)
        .putBoolean("webSearch", s.webSearch)
        .putBoolean("showClock", s.showClock)
        .putBoolean("dynamic", s.dynamicColor)
        .putString("theme", s.themeMode.name)
        .apply()

    init { migrateFromFirstVersion() }

    /**
     * Version 1 kept launch counts in one "usage" string (key=count joined by U+001F) and had
     * favorites, hidden apps and more settings. Counts move to "use:<key>"; the rest is dropped.
     */
    private fun migrateFromFirstVersion() {
        val old = prefs.getString("usage", null) ?: return
        val edit = prefs.edit()
        old.split('\u001F').forEach { entry ->
            val i = entry.lastIndexOf('=')
            val count = entry.substring(i + 1).toIntOrNull()
            if (i > 0 && count != null) edit.putInt("use:" + entry.substring(0, i), count)
        }
        listOf("usage", "favorites", "hidden", "dim", "haptics", "keyboardOnHome").forEach { edit.remove(it) }
        edit.apply()
    }

    fun uses(): Map<String, Int> = prefs.all.mapNotNull { (k, v) ->
        if (k.startsWith("use:") && v is Int) k.removePrefix("use:") to v else null
    }.toMap()

    fun countUse(key: String, count: Int) = prefs.edit().putInt("use:$key", count).apply()
}
