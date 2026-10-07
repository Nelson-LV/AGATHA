package com.hivend.agatha.domain.model

/** Tipo principal de sensor instalado en el nodo. El texto visible vive en strings.xml. */
enum class SensorType {
    MOTION,
    ACOUSTIC,
    ENVIRONMENT,
    POWER,
}

/**
 * Nodo de sensor mostrado en el mapa de campo (HE-08). [normalizedX]/[normalizedY] ubican
 * el punto dentro del lienzo del mapa (0f..1f en cada eje) mientras la integración con el
 * SDK de Google Maps real (coordenadas lat/lng) no reemplaza el mock-up estático — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Mapa de nodos: mock-up vs. SDK real".
 */
data class SensorNode(
    val id: String,
    val pk: String,
    val site: String,
    val sensorType: SensorType,
    val status: AlertLevel,
    val batteryPercentage: Int,
    /** Minutos desde la última comunicación del nodo con la plataforma. */
    val lastCommunicationMinutesAgo: Int,
    val normalizedX: Float,
    val normalizedY: Float,
)
