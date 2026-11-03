package com.example.launcher

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher.ui.HomeScreen
import com.example.launcher.ui.LauncherTheme

class MainActivity : ComponentActivity() {
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is Effect.Launch -> runCatching { vm.source.launch(effect.app) }
                            .onSuccess { vm.launched(effect.app) }
                            .onFailure { toast("${effect.app.label} açılamadı") }
                        is Effect.WebSearch -> webSearch(effect.query)
                    }
                }
            }
            LauncherTheme(state.settings) { HomeScreen(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onReturn()
    }

    override fun onPause() {
        super.onPause()
        vm.onLeave()
    }

    /** Home pressed while this launcher is already in front: back to an empty search box. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.startOver()
    }

    private fun webSearch(query: String) {
        try {
            startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))
            vm.webSearched()
        } catch (_: ActivityNotFoundException) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
                vm.webSearched()
            }.onFailure { toast("Web araması açılamadı") }
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
