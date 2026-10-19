package com.hivend.agatha

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hivend.agatha.core.navigation.AgathaApp
import com.hivend.agatha.push.AlertNotifier
import com.hivend.agatha.ui.theme.AgathaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app (patrón Single-Activity + Jetpack Navigation Compose — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Single-Activity"). Todo el árbol de pantallas vive dentro
 * de [AgathaApp] como composables, nunca como Activities/Fragments adicionales.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Alerta que pidió abrir un push (HU-4.1); se consume al navegar a su detalle. */
    private var alertToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Al recrearse (rotación) el intent original ya se atendió: no volver a abrir el detalle.
        if (savedInstanceState == null) alertToOpen = intent.alertIdExtra()
        setContent {
            AgathaTheme {
                AgathaApp(
                    alertToOpen = alertToOpen,
                    onAlertOpened = { alertToOpen = null },
                )
            }
        }
    }

    /** Push tocado con la app ya abierta (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.alertIdExtra()?.let { alertToOpen = it }
    }

    private fun Intent.alertIdExtra(): String? = getStringExtra(AlertNotifier.EXTRA_ALERT_ID)
}
