package com.emon.proxagallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.emon.proxagallery.ui.HomeScreen
import com.emon.proxagallery.ui.theme.ProxaGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProxaGalleryTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()

                }
            }
        }
    }
}

