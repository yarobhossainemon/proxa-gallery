package com.emon.proxagallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emon.proxagallery.data.AccentColor
import com.emon.proxagallery.data.SettingsRepository
import com.emon.proxagallery.data.ThemeMode
import com.emon.proxagallery.ui.GalleryNavHost
import com.emon.proxagallery.ui.theme.ProxaGalleryTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM_DEFAULT)
            val accentColor by settingsRepository.accentColor
                .collectAsStateWithLifecycle(initialValue = AccentColor.BLUE)

            val isSystemDark = isSystemInDarkTheme()
            val isLightTheme = when (themeMode) {
                ThemeMode.SYSTEM_DEFAULT -> !isSystemDark
                ThemeMode.DARK -> false
                ThemeMode.LIGHT -> true
            }

            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = isLightTheme
            }

            ProxaGalleryTheme(themeMode = themeMode, accentColor = accentColor) {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    GalleryNavHost()

                }
            }
        }
    }
}
