package com.hivend.agatha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hivend.agatha.core.navigation.AgathaApp
import com.hivend.agatha.ui.theme.AgathaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app (patrón Single-Activity + Jetpack Navigation Compose — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Single-Activity"). Todo el árbol de pantallas vive dentro
 * de [AgathaApp] como composables, nunca como Activities/Fragments adicionales.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgathaTheme {
                AgathaApp()
            }
        }
    }
}
