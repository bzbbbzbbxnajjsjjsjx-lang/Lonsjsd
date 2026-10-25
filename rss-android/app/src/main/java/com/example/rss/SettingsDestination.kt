package com.example.rss

/** Every page inside Settings. A page with a [parent] is a sub-page and goes back to it. */
enum class SettingsDestination(val title: String, val parent: SettingsDestination? = null) {
    Home("Ayarlar"),
    Appearance("Görünüm", Home),
    Reading("Okuma", Home),
    Maintenance("Bakım", Home),
    About("Hakkında", Home),
}
