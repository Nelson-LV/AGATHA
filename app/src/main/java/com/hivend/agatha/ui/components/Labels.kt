package com.hivend.agatha.ui.components

import androidx.annotation.StringRes
import com.hivend.agatha.R
import com.hivend.agatha.core.navigation.AgathaDestination.BottomTab
import com.hivend.agatha.domain.model.AccelerationLevel
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ManagementStatus
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.SensorType
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.EventType
import com.hivend.agatha.domain.model.InspectionResult
import com.hivend.agatha.domain.model.Notice

/*
 * Textos visibles de los enums del dominio y de la navegación. El dominio no conoce
 * idiomas: cada pantalla traduce el enum a un recurso de res/values(-en)/strings.xml con
 * estas funciones y lo resuelve con stringResource(). Ver CLAUDE.md § "User-facing strings".
 */

@StringRes
fun AlertLevel.labelRes(): Int = when (this) {
    AlertLevel.RED -> R.string.alert_level_red
    AlertLevel.ORANGE -> R.string.alert_level_orange
    AlertLevel.YELLOW -> R.string.alert_level_yellow
    AlertLevel.GREEN -> R.string.alert_level_green
}

/** Nombre corto del nivel, como en los chips de la web ("Rojo", "Naranja", "Amarillo", "Normal"). */
@StringRes
fun AlertLevel.shortLabelRes(): Int = when (this) {
    AlertLevel.RED -> R.string.level_red
    AlertLevel.ORANGE -> R.string.level_orange
    AlertLevel.YELLOW -> R.string.level_yellow
    AlertLevel.GREEN -> R.string.level_normal
}

/** Motivo del aviso gris (RN-02). */
@StringRes
fun Notice.labelRes(): Int = when (this) {
    Notice.NO_COMMUNICATION -> R.string.notice_no_communication
    Notice.INDICATOR_UNAVAILABLE -> R.string.notice_indicator_unavailable
}

@StringRes
fun AlertEvent.labelRes(): Int = when (this) {
    AlertEvent.LEAK -> R.string.alert_event_leak
    AlertEvent.MOVEMENT -> R.string.alert_event_movement
    AlertEvent.LEAK_AND_MOVEMENT -> R.string.alert_event_leak_and_movement
}

@StringRes
fun AlertClassification.labelRes(): Int = when (this) {
    AlertClassification.CONFIRMED -> R.string.classification_confirmed
    AlertClassification.FALSE_ALARM -> R.string.classification_false_alarm
}

@StringRes
fun ManagementStatus.labelRes(): Int = when (this) {
    ManagementStatus.UNCLASSIFIED -> R.string.management_status_unclassified
    ManagementStatus.CLASSIFIED_WITHOUT_TAG -> R.string.management_status_classified_without_tag
    ManagementStatus.CLASSIFIED_WITH_TAG -> R.string.management_status_classified_with_tag
}

/** Etiquetas RN-07. Las dos variantes "Otro" comparten texto; su detalle lo escribe una persona. */
@StringRes
fun AlertTag.labelRes(): Int = when (this) {
    AlertTag.LEAK_CONFIRMED_IN_FIELD -> R.string.alert_tag_leak_confirmed_in_field
    AlertTag.GROUND_MOVEMENT -> R.string.alert_tag_ground_movement
    AlertTag.THIRD_PARTY_MACHINERY -> R.string.alert_tag_third_party_machinery
    AlertTag.EXTREME_WEATHER -> R.string.alert_tag_extreme_weather
    AlertTag.HEAVY_VEHICLE_TRAFFIC -> R.string.alert_tag_heavy_vehicle_traffic
    AlertTag.SCHEDULED_MAINTENANCE -> R.string.alert_tag_scheduled_maintenance
    AlertTag.OTHER_CONFIRMED, AlertTag.OTHER_FALSE_ALARM -> R.string.common_other_which
}

/** Origen de un registro (RN-10). [Origin.MobileApp] lleva el id del celular como `%1$s`. */
@StringRes
fun Origin.labelRes(): Int = when (this) {
    Origin.Web -> R.string.origin_web
    is Origin.MobileApp -> R.string.origin_mobile_app
}

/** Estado de un dispositivo según el nivel de su último reporte (verde = normal). */
@StringRes
fun AlertLevel.deviceStatusRes(): Int = when (this) {
    AlertLevel.RED -> R.string.device_status_critical
    AlertLevel.ORANGE, AlertLevel.YELLOW -> R.string.device_status_alert
    AlertLevel.GREEN -> R.string.device_status_normal
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
    InspectionResult.NO_ISSUES -> R.string.inspection_result_no_issues
    InspectionResult.MAINTENANCE_REQUIRED -> R.string.inspection_result_maintenance_required
    InspectionResult.DEVICE_OR_SENSOR_PROBLEM -> R.string.inspection_result_device_or_sensor_problem
    InspectionResult.PHYSICAL_DAMAGE -> R.string.inspection_result_physical_damage
    InspectionResult.LOW_BATTERY -> R.string.inspection_result_low_battery
    InspectionResult.NO_COMMUNICATION -> R.string.inspection_result_no_communication
    InspectionResult.OTHER -> R.string.common_other_which
}

@StringRes
fun MaintenanceType.labelRes(): Int = when (this) {
    MaintenanceType.BATTERY_CHANGE_OR_RECHARGE -> R.string.maintenance_type_battery
    MaintenanceType.CLEANING -> R.string.maintenance_type_cleaning
    MaintenanceType.ADJUSTMENT_OR_REINSTALLATION -> R.string.maintenance_type_adjustment
    MaintenanceType.COMPONENT_REPLACEMENT -> R.string.maintenance_type_component_replacement
    MaintenanceType.OTHER -> R.string.common_other_which
}

@StringRes
fun AccelerationLevel.labelRes(): Int = when (this) {
    AccelerationLevel.LOW -> R.string.acceleration_low
    AccelerationLevel.MEDIUM -> R.string.acceleration_medium
    AccelerationLevel.HIGH -> R.string.acceleration_high
}

@StringRes
fun SensorType.labelRes(): Int = when (this) {
    SensorType.MOTION -> R.string.sensor_type_motion
    SensorType.ACOUSTIC -> R.string.sensor_type_acoustic
    SensorType.ENVIRONMENT -> R.string.sensor_type_environment
    SensorType.POWER -> R.string.sensor_type_power
}

@StringRes
fun BottomTab.labelRes(): Int = when (this) {
    BottomTab.Alerts -> R.string.tab_alerts
    BottomTab.Devices -> R.string.tab_devices
    BottomTab.Map -> R.string.tab_map
    BottomTab.History -> R.string.tab_history
    BottomTab.Sync -> R.string.tab_sync
}
