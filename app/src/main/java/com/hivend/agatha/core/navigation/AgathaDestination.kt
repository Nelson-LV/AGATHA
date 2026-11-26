package com.hivend.agatha.core.navigation

import android.net.Uri

/**
 * Rutas de navegación de AGATHA como una jerarquía sellada (en vez de strings sueltos
 * repetidos por toda la UI). [BottomTab] son los 5 destinos con barra inferior visible
 * (Alertas, Dispositivos, Mapa, Historial, Sync); el resto se apilan a pantalla completa con
 * flecha de regreso, igual que en Figma. Ver docs/ARQUITECTURA_Y_DISENO.md § "Navegación".
 */
sealed class AgathaDestination(val route: String) {

    sealed class BottomTab(route: String) : AgathaDestination(route) {
        data object Alerts : BottomTab("alerts")
        data object Devices : BottomTab("devices")
        data object Map : BottomTab("map")
        data object History : BottomTab("history/{deviceId}") {
            const val ARG_DEVICE_ID = "deviceId"
            fun buildRoute(deviceId: String) = "history/$deviceId"
        }
        data object Sync : BottomTab("sync")

        companion object {
            val all = listOf(Alerts, Devices, Map, History, Sync)
        }
    }

    data object AlertDetail : AgathaDestination("alert_detail/{alertId}?focus={focus}") {
        const val ARG_ALERT_ID = "alertId"

        /** Desde un push: el `messageId` al que ir, o [FOCUS_CURRENT] para el tramo vigente. */
        const val ARG_FOCUS = "focus"
        const val FOCUS_CURRENT = "current"

        /** El id puede venir de un push: sin codificar, un "/" o "?" haría fallar la navegación. */
        fun buildRoute(alertId: String, focus: String? = null) =
            "alert_detail/${Uri.encode(alertId)}" + (focus?.let { "?focus=${Uri.encode(it)}" } ?: "")
    }

    data object InspectionForm : AgathaDestination("inspection_form/{alertId}") {
        const val ARG_ALERT_ID = "alertId"
        fun buildRoute(alertId: String) = "inspection_form/$alertId"
    }
}

/** Alerta que pidió abrir un push tocado (HU-4.1) y, si vino, el mensaje al que llevar el detalle. */
data class AlertOpenRequest(val alertId: String, val messageId: String? = null)

/** Route marker used by the Historial tab; its ViewModel restores the last sensor selection. */
const val HISTORY_LAST_SELECTED_DEVICE_ID = "_last"
