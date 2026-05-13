package dev.a10101100.snipcraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.a10101100.snipcraft.core.accessibility.SnipForegroundService
import dev.a10101100.snipcraft.core.designsystem.SnipTheme
import dev.a10101100.snipcraft.navigation.SnipNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startForegroundCompanion()
        setContent {
            SnipTheme {
                SnipNavHost()
            }
        }
    }

    private fun startForegroundCompanion() {
        ContextCompat.startForegroundService(this, SnipForegroundService.startIntent(this))
    }
}
