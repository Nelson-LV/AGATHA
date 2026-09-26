package com.hivend.agatha.domain.model

/**
 * Nodo de sensor mostrado en el mapa de campo (HE-08). [xNormalizado]/[yNormalizado] ubican
 * el punto dentro del lienzo del mapa (0f..1f en cada eje) mientras la integración con el
 * SDK de Google Maps real (coordenadas lat/lng) no reemplaza el mock-up estático — ver
 * docs/ARQUITECTURA_Y_DISENO.md § "Mapa de nodos: mock-up vs. SDK real".
 */
data class SensorNode(
    val id: String,
    val name: String,
    val pk: String,
    val site: String,
    val sensorType: String,
    val status: AlertLevel,
    val batteryPercentage: Int,
    val lastCommunication: String,
    val normalizedX: Float,
    val normalizedY: Float,
)
