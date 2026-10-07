package com.hivend.agatha.domain.model

/**
 * Resultado observado sobre el dispositivo/sensor durante la inspección (HU-5.1). El texto
 * visible vive en strings.xml — ver [com.hivend.agatha.ui.components.labelRes].
 */
enum class InspectionResult {
    NO_ISSUE,
    MAINTENANCE_REQUIRED,
    DEVICE_ISSUE,
    PHYSICAL_DAMAGE,
    LOW_BATTERY,
    NO_COMMUNICATION,
    OTHER,
}

/** Categoría configurable que clasifica la causa real de la alerta (HU-5.2). */
enum class EventCategory {
    GROUND_MOVEMENT,
    MACHINERY_INTERVENTION,
    WEATHER_CONDITIONS,
    HEAVY_VEHICLE_TRAFFIC,
    NO_ANOMALIES,
    SCHEDULED_MAINTENANCE,
    CONFIRMED_LEAK,
    OTHER,
}

/**
 * Registro de inspección de campo (HE-05). Se guarda localmente primero (offline-first,
 * HE-07) y se sincroniza cuando hay conectividad — ver [com.hivend.agatha.data.sync].
 */
data class Inspection(
    val alertId: String,
    val result: InspectionResult,
    val category: EventCategory,
    val observations: String,
    val recordedOffline: Boolean,
)
