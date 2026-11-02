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
import com.example.launcher.ui.LauncherScreen
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
                            .onFailure { toast("${effect.app.label} açılamadı") }
                        is Effect.WebSearch -> webSearch(effect.query)
                        is Effect.Toast -> toast(effect.text)
                    }
                }
            }
            LauncherTheme(state.settings) { LauncherScreen(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    /** Home pressed while the launcher is already in front. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.onHome()
    }

    private fun webSearch(query: String) {
        try {
            startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query))
        } catch (_: ActivityNotFoundException) {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
            }.onFailure { toast("Web araması açılamadı") }
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
