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

    data object AlertDetail : AgathaDestination("alert_detail/{alertId}") {
        const val ARG_ALERT_ID = "alertId"

        /** El id puede venir de un push: sin codificar, un "/" o "?" haría fallar la navegación. */
        fun buildRoute(alertId: String) = "alert_detail/${Uri.encode(alertId)}"
    }

    data object InspectionForm : AgathaDestination("inspection_form/{alertId}") {
        const val ARG_ALERT_ID = "alertId"
        fun buildRoute(alertId: String) = "inspection_form/$alertId"
    }
}

/** El historial por defecto que abre el tab inferior "Historial" antes de elegir un nodo. */
const val DEFAULT_HISTORY_DEVICE_ID = "MP-11556"

/** Route marker used by the Historial tab; its ViewModel restores the last sensor selection. */
const val HISTORY_LAST_SELECTED_DEVICE_ID = "_last"
