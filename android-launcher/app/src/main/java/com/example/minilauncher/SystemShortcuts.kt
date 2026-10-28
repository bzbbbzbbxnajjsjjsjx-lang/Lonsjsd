package com.example.minilauncher

import android.content.Intent
import android.provider.Settings

/** A system settings page reachable by typing one of its keywords, such as "wifi" or "pil". */
data class SystemShortcut(val labelRes: Int, val keywords: List<String>, val action: String)

val systemShortcuts = listOf(
    SystemShortcut(R.string.sys_wifi, listOf("wifi", "wi-fi", "internet"), Settings.ACTION_WIFI_SETTINGS),
    SystemShortcut(R.string.sys_bluetooth, listOf("bluetooth"), Settings.ACTION_BLUETOOTH_SETTINGS),
    SystemShortcut(R.string.sys_display, listOf("ekran", "parlaklık", "display"), Settings.ACTION_DISPLAY_SETTINGS),
    SystemShortcut(R.string.sys_sound, listOf("ses", "sound", "zil"), Settings.ACTION_SOUND_SETTINGS),
    SystemShortcut(R.string.sys_location, listOf("konum", "location", "gps"), Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    SystemShortcut(R.string.sys_battery, listOf("pil", "batarya", "battery"), Intent.ACTION_POWER_USAGE_SUMMARY),
    SystemShortcut(R.string.sys_apps, listOf("uygulama ayarları", "uygulamalar"), Settings.ACTION_APPLICATION_SETTINGS),
)

/** Shortcuts with a keyword that starts with the typed text. Needs at least two letters. */
fun matchingSystemShortcuts(query: String): List<SystemShortcut> {
    val q = query.trim().lowercase()
    if (q.length < 2) return emptyList()
    return systemShortcuts.filter { shortcut -> shortcut.keywords.any { it.startsWith(q) } }
}
