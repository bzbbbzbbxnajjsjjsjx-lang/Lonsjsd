package com.example.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notes.ui.NotesApp
import com.example.notes.ui.NotesTheme

class MainActivity : ComponentActivity() {
    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !viewModel.state.value.loaded }
        enableEdgeToEdge()
        if (savedInstanceState == null) handle(intent)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            NotesTheme(state.settings) {
                NotesApp(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.flush()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Shortcuts, shared text and reminder taps all arrive here. */
    private fun handle(intent: Intent?) {
        intent ?: return
        when {
            intent.action == "com.example.notes.NEW_NOTE" -> viewModel.create()
            intent.action == "com.example.notes.NEW_LIST" -> viewModel.create(checklist = true)
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                viewModel.create(body = text, title = subject)
            }
            intent.hasExtra(Reminders.EXTRA_NOTE_ID) -> intent.getStringExtra(Reminders.EXTRA_NOTE_ID)?.let(viewModel::open)
        }
    }
}
