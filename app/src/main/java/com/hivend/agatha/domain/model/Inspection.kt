package com.hivend.agatha.domain.model

/**
 * Lo que el técnico observa durante la inspección (HU-5.1). La inspección es visual y
 * superficial: el personal de campo no tiene acceso directo a la tubería de gas, así que
 * solo se registra lo que se ve en superficie (terreno, entorno) y en el dispositivo. El
 * texto visible vive en strings.xml — ver [com.hivend.agatha.ui.components.labelRes].
 */
enum class InspectionResult {
    NO_VISIBLE_ISSUE,
    GROUND_ALTERATION,
    SURFACE_GAS_SIGNS,
    VISIBLE_DEVICE_DAMAGE,
    MAINTENANCE_REQUIRED,
    LOW_BATTERY,
    NO_COMMUNICATION,
    OTHER,
}

/**
 * Categoría configurable que clasifica la causa de la alerta (HU-5.2). Una fuga no puede
 * confirmarse sin acceso a la tubería, por eso la categoría es [SUSPECTED_LEAK]: la
 * confirmación queda en manos del operador.
 */
enum class EventCategory {
    GROUND_MOVEMENT,
    MACHINERY_INTERVENTION,
    WEATHER_CONDITIONS,
    HEAVY_VEHICLE_TRAFFIC,
    NO_ANOMALIES,
    SCHEDULED_MAINTENANCE,
    SUSPECTED_LEAK,
    OTHER,
}

/**
 * Registro de inspección de campo (HE-05): clasificación de la alerta y lo observado en
 * superficie. Las observaciones libres no viven aquí sino en [Alert.observations], porque el
 * técnico puede escribirlas antes, durante o sin inspección. Se guarda localmente primero
 * (offline-first, HE-07) y se sincroniza cuando hay conectividad — ver
 * [com.hivend.agatha.data.sync].
 */
data class Inspection(
    val alertId: String,
    val result: InspectionResult,
    val category: EventCategory,
    val recordedOffline: Boolean,
)
