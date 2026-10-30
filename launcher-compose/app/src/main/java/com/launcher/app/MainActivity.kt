package com.launcher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ana ekran launcher'ında geri tuşu uygulamayı kapatmaz; ana ekranda kalır.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Bilerek boş: launcher kökünde geri tuşu hiçbir şey yapmaz.
            }
        })

        setContent {
            MaterialExpressiveTheme(
                colorScheme = LauncherColors,
                motionScheme = MotionScheme.standard(),
            ) {
                LauncherScreen()
            }
        }
    }
}
