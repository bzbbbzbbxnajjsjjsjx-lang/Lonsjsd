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
    /** Apps matching [query], best first. Empty while nothing is typed. */
    val matches: List<App> = emptyList(),
    val settings: Settings = Settings(),
    val settingsOpen: Boolean = false,
    /** Bumped when installed apps change, so an icon on screen is fetched again. */
    val iconEpoch: Int = 0,
) {
    val best: App? get() = matches.firstOrNull()
}

sealed interface Effect {
    data class Launch(val app: App) : Effect
    data class WebSearch(val query: String) : Effect
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val source = AppSource(application)
    private val store = LauncherStore(application)
    private val matcher = AppMatcher()
    private val uses = store.uses().toMutableMap()

    private val _state = MutableStateFlow(LauncherState(settings = store.settings()))
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    private val effects = Channel<Effect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    /** Bumped whenever the home screen should start over: empty box, keyboard up. */
    private val _reset = MutableStateFlow(0)
    val reset: StateFlow<Int> = _reset.asStateFlow()

    private var autoLaunch: Job? = null
    private var reloadJob: Job? = null
    private val watcher = source.watch {
        source.clearIcons()
        _state.update { it.copy(iconEpoch = it.iconEpoch + 1) }
        reload(debounce = true)
    }

    /** Set when the launcher hands off to another app, so coming back starts clean. */
    var handedOff = false
        private set

    init { reload(debounce = false) }

    /** One scan at a time; a burst of package changes collapses into the last one. */
    private fun reload(debounce: Boolean) {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            if (debounce) delay(250)
            val apps = withContext(Dispatchers.IO) { runCatching { source.load() }.getOrNull() } ?: return@launch
            _state.update { it.copy(apps = apps, matches = rank(it.query, apps)) }
        }
    }

    private fun rank(query: String, apps: List<App>) =
        if (query.isBlank()) emptyList() else matcher.rank(query, apps, { it.label }, { uses[it.key] ?: 0 })

    fun setQuery(query: String) {
        autoLaunch?.cancel()
        val matches = rank(query, state.value.apps)
        _state.update { it.copy(query = query, matches = matches) }
        // Exactly one app fits what was typed: open it after a short pause, which a fast
        // typist's next letter cancels. One letter is too little to be sure.
        if (state.value.settings.autoLaunch && matches.size == 1 && query.trim().length >= 2) {
            autoLaunch = viewModelScope.launch {
                delay(AUTO_LAUNCH_DELAY_MS)
                if (state.value.query == query) open(matches.single())
            }
        }
    }

    /** The keyboard's Go key. */
    fun submit() {
        val s = state.value
        when {
            s.best != null -> open(s.best!!)
            s.query.isNotBlank() && s.settings.webSearch -> effects.trySend(Effect.WebSearch(s.query.trim()))
        }
    }

    private fun open(app: App) {
        autoLaunch?.cancel()
        effects.trySend(Effect.Launch(app))
    }

    /** Called once the app really started; only then does it count toward ranking. */
    fun launched(app: App) {
        handedOff = true
        val count = (uses[app.key] ?: 0) + 1
        uses[app.key] = count
        store.countUse(app.key, count)
    }

    fun webSearched() { handedOff = true }

    /** The launcher left the foreground: an auto-launch still waiting must not fire. */
    fun onLeave() = autoLaunch?.cancel()

    fun onReturn() {
        if (handedOff) {
            handedOff = false
            startOver()
        }
    }

    /** Home pressed, or the launcher came back after opening something. */
    fun startOver() {
        autoLaunch?.cancel()
        _state.update { it.copy(query = "", matches = emptyList(), settingsOpen = false) }
        _reset.update { it + 1 }
    }

    fun openSettings(open: Boolean) = _state.update { it.copy(settingsOpen = open) }

    fun updateSettings(change: (Settings) -> Settings) {
        _state.update { it.copy(settings = change(it.settings)) }
        store.save(state.value.settings)
    }

    override fun onCleared() = source.unwatch(watcher)

    companion object {
        const val AUTO_LAUNCH_DELAY_MS = 300L
    }
}
