package com.example.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.UserHandle
import android.os.UserManager
import androidx.core.graphics.drawable.toBitmap

/** One launchable activity. [key] is unique across work and personal profiles. */
data class App(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWork: Boolean,
) {
    val packageName: String get() = component.packageName
    val key: String = component.flattenToString() + "#" + user.hashCode()
}

/** Reads launchable apps through LauncherApps, for every profile on the device. */
class AppSource(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val iconCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()
    private val collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("tr"))

    fun load(): List<App> {
        val self = android.os.Process.myUserHandle()
        val ownPackage = context.packageName
        return launcherApps.profiles
            .filter { runCatching { !userManager.isQuietModeEnabled(it) }.getOrDefault(true) }
            .flatMap { user ->
                launcherApps.getActivityList(null, user).map {
                    App(it.label.toString(), it.componentName, user, isWork = user != self)
                }
            }
            .filterNot { it.packageName == ownPackage && !it.isWork }
            .sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun icon(app: App, sizePx: Int): Bitmap? = iconCache["${app.key}@$sizePx"] ?: runCatching {
        val info = launcherApps.resolveActivity(
            android.content.Intent(android.content.Intent.ACTION_MAIN).setComponent(app.component), app.user,
        ) ?: return null
        info.getBadgedIcon(context.resources.displayMetrics.densityDpi).toBitmap(sizePx, sizePx)
    }.getOrNull()?.also { iconCache["${app.key}@$sizePx"] = it }

    fun forget(packageName: String) {
        iconCache.keys.removeAll { it.startsWith("$packageName/") }
    }

    fun launch(app: App, bounds: android.graphics.Rect? = null) {
        launcherApps.startMainActivity(app.component, app.user, bounds, null)
    }

    fun openDetails(app: App) {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }

    fun register(onChange: (String) -> Unit): LauncherApps.Callback {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String, user: UserHandle) = onChange(packageName)
            override fun onPackageAdded(packageName: String, user: UserHandle) = onChange(packageName)
            override fun onPackageChanged(packageName: String, user: UserHandle) = onChange(packageName)
            override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(onChange)
            override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                packageNames.forEach(onChange)
        }
        launcherApps.registerCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
        return callback
    }

    fun unregister(callback: LauncherApps.Callback) = launcherApps.unregisterCallback(callback)
}

enum class ThemeMode { System, Light, Dark }

data class Settings(
    /** Open the app as soon as the typed text matches exactly one app. */
    val autoLaunch: Boolean = true,
    /** Show the keyboard whenever the home screen appears. */
    val keyboardOnHome: Boolean = true,
    val showClock: Boolean = true,
    val dynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    /** How much the wallpaper is dimmed behind the home screen, 0..0.8. */
    val dim: Float = 0.25f,
    val haptics: Boolean = true,
    val webSearch: Boolean = true,
)

/** Settings, favorites, hidden apps and launch counts, in SharedPreferences. */
class LauncherPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    private val sep = "\u001F"

    fun settings() = Settings(
        autoLaunch = prefs.getBoolean("autoLaunch", true),
        keyboardOnHome = prefs.getBoolean("keyboardOnHome", true),
        showClock = prefs.getBoolean("showClock", true),
        dynamicColor = prefs.getBoolean("dynamic", true),
        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("theme", null) } ?: ThemeMode.System,
        dim = prefs.getFloat("dim", 0.25f),
        haptics = prefs.getBoolean("haptics", true),
        webSearch = prefs.getBoolean("webSearch", true),
    )

    fun save(s: Settings) = prefs.edit()
        .putBoolean("autoLaunch", s.autoLaunch)
        .putBoolean("keyboardOnHome", s.keyboardOnHome)
        .putBoolean("showClock", s.showClock)
        .putBoolean("dynamic", s.dynamicColor)
        .putString("theme", s.themeMode.name)
        .putFloat("dim", s.dim)
        .putBoolean("haptics", s.haptics)
        .putBoolean("webSearch", s.webSearch)
        .apply()

    fun favorites(): List<String> = prefs.getString("favorites", "")!!.split(sep).filter { it.isNotEmpty() }
    fun saveFavorites(keys: List<String>) = prefs.edit().putString("favorites", keys.joinToString(sep)).apply()

    fun hidden(): Set<String> = prefs.getStringSet("hidden", emptySet())!!.toSet()
    fun saveHidden(keys: Set<String>) = prefs.edit().putStringSet("hidden", keys).apply()

    fun usage(): Map<String, Int> = prefs.getString("usage", "")!!.split(sep).mapNotNull { entry ->
        val i = entry.lastIndexOf('=')
        if (i <= 0) null else entry.substring(0, i) to (entry.substring(i + 1).toIntOrNull() ?: return@mapNotNull null)
    }.toMap()

    fun saveUsage(usage: Map<String, Int>) =
        prefs.edit().putString("usage", usage.entries.joinToString(sep) { "${it.key}=${it.value}" }).apply()
}
