package com.hivend.agatha.ui.components

import androidx.annotation.StringRes
import com.hivend.agatha.R
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.EventCategory
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.InspectionResult

/*
 * Textos visibles de los enums del dominio y de la navegación. El dominio no conoce
 * idiomas: cada pantalla traduce el enum a un recurso de res/values(-en)/strings.xml con
 * estas funciones y lo resuelve con stringResource(). Ver CLAUDE.md § "User-facing strings".
 */

@StringRes
fun AlertStatus.labelRes(): Int = when (this) {
    AlertStatus.GENERATED -> R.string.alert_status_generated
    AlertStatus.RECEIVED -> R.string.alert_status_received
    AlertStatus.IN_INSPECTION -> R.string.alert_status_in_inspection
    AlertStatus.CLASSIFIED -> R.string.alert_status_classified
    AlertStatus.CLOSED -> R.string.alert_status_closed
}

@StringRes
fun AlertLevel.labelRes(): Int = when (this) {
    AlertLevel.RED -> R.string.alert_level_red
    AlertLevel.YELLOW -> R.string.alert_level_yellow
    AlertLevel.GREEN -> R.string.alert_level_green
}

@StringRes
fun EventType.labelRes(): Int = when (this) {
    EventType.ALERT -> R.string.event_type_alert
    EventType.INSPECTION -> R.string.event_type_inspection
    EventType.CLASSIFICATION -> R.string.event_type_classification
    EventType.EVIDENCE -> R.string.event_type_evidence
    EventType.OBSERVATION -> R.string.event_type_observation
    EventType.MAINTENANCE -> R.string.event_type_maintenance
}

@StringRes
fun InspectionResult.labelRes(): Int = when (this) {
    InspectionResult.NO_ISSUE -> R.string.inspection_result_no_issue
    InspectionResult.MAINTENANCE_REQUIRED -> R.string.inspection_result_maintenance_required
    InspectionResult.DEVICE_ISSUE -> R.string.inspection_result_device_issue
    InspectionResult.PHYSICAL_DAMAGE -> R.string.inspection_result_physical_damage
    InspectionResult.LOW_BATTERY -> R.string.inspection_result_low_battery
    InspectionResult.NO_COMMUNICATION -> R.string.inspection_result_no_communication
    InspectionResult.OTHER -> R.string.inspection_result_other
}

@StringRes
fun EventCategory.labelRes(): Int = when (this) {
    EventCategory.GROUND_MOVEMENT -> R.string.event_category_ground_movement
    EventCategory.MACHINERY_INTERVENTION -> R.string.event_category_machinery_intervention
    EventCategory.WEATHER_CONDITIONS -> R.string.event_category_weather_conditions
    EventCategory.HEAVY_VEHICLE_TRAFFIC -> R.string.event_category_heavy_vehicle_traffic
    EventCategory.NO_ANOMALIES -> R.string.event_category_no_anomalies
    EventCategory.SCHEDULED_MAINTENANCE -> R.string.event_category_scheduled_maintenance
    EventCategory.CONFIRMED_LEAK -> R.string.event_category_confirmed_leak
    EventCategory.OTHER -> R.string.event_category_other
}

@StringRes
fun BottomTab.labelRes(): Int = when (this) {
    BottomTab.Alerts -> R.string.tab_alerts
    BottomTab.Map -> R.string.tab_map
    BottomTab.History -> R.string.tab_history
    BottomTab.Sync -> R.string.tab_sync
}
