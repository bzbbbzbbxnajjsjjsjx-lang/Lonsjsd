package com.example.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherState(
    val apps: List<App> = emptyList(),
    val query: String = "",
    val settings: Settings = Settings(),
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val usage: Map<String, Int> = emptyMap(),
    val drawerOpen: Boolean = false,
    val settingsOpen: Boolean = false,
    val loaded: Boolean = false,
    /** Bumped when installed packages change, so icons on screen are fetched again. */
    val iconEpoch: Int = 0,
) {
    val visibleApps: List<App> get() = apps.filterNot { it.key in hidden }

    /**
     * Apps matching the query, best first; ties go to the app opened most often. Hidden apps
     * stay out of the drawer but can still be opened by typing their name.
     */
    val results: List<App> by lazy {
            if (query.isBlank()) return@lazy emptyList()
            apps
                .map { it to Search.score(it.label, query, it.packageName) }
                .filter { it.second > 0 }
                .sortedWith(
                    compareByDescending<Pair<App, Int>> { it.second }
                        .thenByDescending { usage[it.first.key] ?: 0 }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.label },
                )
                .map { it.first }
        }

    val favoriteApps: List<App> get() = favorites.mapNotNull { k -> visibleApps.firstOrNull { it.key == k } }

    /** Most opened apps that are not already favorites, for the home screen. */
    val suggestions: List<App>
        get() = visibleApps.filter { it.key !in favorites && (usage[it.key] ?: 0) > 0 }
            .sortedByDescending { usage[it.key] ?: 0 }
            .take(4)

    val calculation: String? get() = Calculator.evaluate(query)?.let(Calculator::format)
}

sealed interface Effect {
    data class Launch(val app: App) : Effect
    data class WebSearch(val query: String) : Effect
    data class Toast(val text: String) : Effect
}

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val source = AppSource(app)
    private val prefs = LauncherPrefs(app)

    private val _state = MutableStateFlow(LauncherState())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private val effects = Channel<Effect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    /** Bumped every time the home screen comes to the front, to refocus the search box. */
    private val _homeShown = MutableStateFlow(0)
    val homeShown: StateFlow<Int> = _homeShown.asStateFlow()

    private var autoLaunchJob: Job? = null
    private var reloadJob: Job? = null
    private val callback = source.register { pkg ->
        source.forget(pkg)
        _state.update { it.copy(iconEpoch = it.iconEpoch + 1) }
        reload()
    }

    /** Set when the launcher hands off to another app, so coming back starts clean. */
    private var leftHome = false

    init {
        _state.update {
            it.copy(settings = prefs.settings(), favorites = prefs.favorites(), hidden = prefs.hidden(), usage = prefs.usage())
        }
        reload()
    }

    /** One scan at a time; a burst of package changes collapses into the last one. */
    fun reload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            if (state.value.loaded) delay(250)
            val apps = withContext(Dispatchers.IO) { runCatching { source.load() }.getOrNull() } ?: return@launch
            _state.update { it.copy(apps = apps, loaded = true) }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        autoLaunchJob?.cancel()
        val s = state.value
        if (!s.settings.autoLaunch || q.isBlank()) return
        val results = s.results
        // One match left: open it after a short pause, so a fast typist can still keep going.
        if (results.size == 1) {
            val only = results.first()
            autoLaunchJob = viewModelScope.launch {
                delay(350)
                if (state.value.query == q) launch(only)
            }
        }
    }

    /** The keyboard's Go key: open the best match, else search the web. */
    fun submit() {
        val s = state.value
        val top = s.results.firstOrNull()
        when {
            top != null -> launch(top)
            s.query.isNotBlank() && s.settings.webSearch -> webSearch()
        }
    }

    fun launch(app: App) {
        autoLaunchJob?.cancel()
        leftHome = true
        effects.trySend(Effect.Launch(app))
        val usage = state.value.usage.toMutableMap().apply { this[app.key] = (this[app.key] ?: 0) + 1 }
        _state.update { it.copy(usage = usage) }
        viewModelScope.launch(Dispatchers.IO) { prefs.saveUsage(usage) }
    }

    fun webSearch() {
        val q = state.value.query.trim()
        if (q.isNotEmpty()) { leftHome = true; effects.trySend(Effect.WebSearch(q)) }
    }

    /** The launcher is back after opening something: start from a clean home screen. */
    fun onResume() {
        if (leftHome) { leftHome = false; onHome() }
    }

    /** Home was pressed: start from a clean home screen. */
    fun onHome() {
        autoLaunchJob?.cancel()
        _state.update { it.copy(query = "", drawerOpen = false, settingsOpen = false) }
        _homeShown.update { it + 1 }
    }

    fun clearQuery() {
        autoLaunchJob?.cancel()
        _state.update { it.copy(query = "") }
    }

    fun openDrawer(open: Boolean) = _state.update { it.copy(drawerOpen = open) }
    fun openSettings(open: Boolean) = _state.update { it.copy(settingsOpen = open) }

    fun toggleFavorite(app: App) {
        val favs = state.value.favorites
        val next = if (app.key in favs) favs - app.key else favs + app.key
        _state.update { it.copy(favorites = next) }
        prefs.saveFavorites(next)
        effects.trySend(Effect.Toast(if (app.key in favs) "${app.label} ana ekrandan kaldırıldı" else "${app.label} ana ekrana eklendi"))
    }

    /** Moves within the favorites the dock actually shows; hidden or removed ones keep their place at the end. */
    fun moveFavorite(app: App, by: Int) {
        val s = state.value
        val shown = s.favoriteApps.map { it.key }.toMutableList()
        val i = shown.indexOf(app.key)
        if (i < 0) return
        val j = (i + by).coerceIn(0, shown.lastIndex)
        if (i == j) return
        shown.add(j, shown.removeAt(i))
        val next = shown + s.favorites.filterNot { it in shown }
        _state.update { it.copy(favorites = next) }
        prefs.saveFavorites(next)
    }

    fun setHidden(app: App, hide: Boolean) {
        val next = if (hide) state.value.hidden + app.key else state.value.hidden - app.key
        _state.update { it.copy(hidden = next) }
        prefs.saveHidden(next)
        if (hide) effects.trySend(Effect.Toast("${app.label} çekmeceden gizlendi; yazarak yine açabilirsiniz"))
    }

    fun updateSettings(f: (Settings) -> Settings) {
        _state.update { it.copy(settings = f(it.settings)) }
        prefs.save(state.value.settings)
    }

    override fun onCleared() = source.unregister(callback)
}
