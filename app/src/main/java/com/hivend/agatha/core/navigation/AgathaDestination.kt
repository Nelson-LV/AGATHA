package com.hivend.agatha.core.navigation

/**
 * Rutas de navegación de AGATHA como una jerarquía sellada (en vez de strings sueltos
 * repetidos por toda la UI). [BottomTab] son los 4 destinos con barra inferior visible en
 * el prototipo (Alertas, Mapa, Historial, Sync); el resto se apilan a pantalla completa con
 * flecha de regreso, igual que en Figma. Ver docs/ARQUITECTURA_Y_DISENO.md § "Navegación".
 */
sealed class AgathaDestination(val route: String) {

    data object NotificationPreview : AgathaDestination("notification_preview")

    sealed class BottomTab(route: String, val label: String) : AgathaDestination(route) {
        data object Alerts : BottomTab("alerts", "Alertas")
        data object Map : BottomTab("map", "Mapa")
        data object History : BottomTab("history/{deviceId}", "Historial") {
            const val ARG_DEVICE_ID = "deviceId"
            fun buildRoute(deviceId: String) = "history/$deviceId"
        }
        data object Sync : BottomTab("sync", "Sync")

        companion object {
            val all = listOf(Alerts, Map, History, Sync)
        }
    }

    data object AlertDetail : AgathaDestination("alert_detail/{alertId}") {
        const val ARG_ALERT_ID = "alertId"
        fun buildRoute(alertId: String) = "alert_detail/$alertId"
    }

    data object InspectionForm : AgathaDestination("inspection_form/{alertId}") {
        const val ARG_ALERT_ID = "alertId"
        fun buildRoute(alertId: String) = "inspection_form/$alertId"
    }

    data object PhotoEvidence : AgathaDestination("photo_evidence/{alertId}") {
        const val ARG_ALERT_ID = "alertId"
        fun buildRoute(alertId: String) = "photo_evidence/$alertId"
    }
}

/** El historial por defecto que abre el tab inferior "Historial" antes de elegir un nodo. */
const val DEFAULT_HISTORY_DEVICE_ID = "MP-1156"
