package com.hivend.agatha.domain.model

/** Resultado observado sobre el dispositivo/sensor durante la inspección (HU-5.1). */
enum class InspectionResult(val label: String) {
    NO_ISSUE("Sin novedad"),
    MAINTENANCE_REQUIRED("Mantenimiento requerido"),
    DEVICE_ISSUE("Problema del dispositivo/sensor"),
    PHYSICAL_DAMAGE("Daño físico"),
    LOW_BATTERY("Batería baja"),
    NO_COMMUNICATION("Sin comunicación"),
    OTHER("Otra"),
}

/** Categoría configurable que clasifica la causa real de la alerta (HU-5.2). */
enum class EventCategory(val label: String) {
    GROUND_MOVEMENT("Movimiento de tierra o desprendimiento menor"),
    MACHINERY_INTERVENTION("Intervención de maquinaria ajena"),
    WEATHER_CONDITIONS("Condiciones climáticas externas"),
    HEAVY_VEHICLE_TRAFFIC("Tránsito de vehículo pesado"),
    NO_ANOMALIES("Sin anomalías"),
    SCHEDULED_MAINTENANCE("Mantenimiento programado"),
    CONFIRMED_LEAK("Fuga confirmada"),
    OTHER("Otro (especificar)"),
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
