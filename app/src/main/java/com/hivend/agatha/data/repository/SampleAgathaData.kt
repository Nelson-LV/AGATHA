package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.SensorType

/**
 * Datos de muestra que aún usa el mapa de nodos (InMemorySensorNodeRepository). Las alertas y su
 * historial ya salen de la API de gestión de alertas (RemoteAlertRepository). Reproduce los
 * prototipos de Figma (sitio Güepsa – San José de Pare, sensores MP-11xx).
 */
internal object SampleAgathaData {

    const val SITE = "Güepsa – San José de Pare"
    const val MUNICIPALITY = "San José de Pare"

    val nodes: List<SensorNode> = listOf(
        SensorNode(
            id = "MP-11556",
            pk = "PK34+470",
            site = SITE,
            sensorType = SensorType.MOTION,
            status = AlertLevel.RED,
            batteryPercentage = 78,
            lastCommunicationMinutesAgo = 5,
            normalizedX = 0.78f,
            normalizedY = 0.22f,
        ),
        SensorNode(
            id = "MP-1189",
            pk = "PK22+300",
            site = SITE,
            sensorType = SensorType.ACOUSTIC,
            status = AlertLevel.ORANGE,
            batteryPercentage = 64,
            lastCommunicationMinutesAgo = 40,
            normalizedX = 0.45f,
            normalizedY = 0.48f,
        ),
        SensorNode(
            id = "MP-1122",
            pk = "PK10+050",
            site = SITE,
            sensorType = SensorType.ENVIRONMENT,
            status = AlertLevel.ORANGE,
            batteryPercentage = 91,
            lastCommunicationMinutesAgo = 120,
            normalizedX = 0.20f,
            normalizedY = 0.75f,
        ),
        SensorNode(
            id = "MP-1201",
            pk = "PK44+120",
            site = SITE,
            sensorType = SensorType.POWER,
            status = AlertLevel.YELLOW,
            batteryPercentage = 85,
            lastCommunicationMinutesAgo = 180,
            normalizedX = 0.83f,
            normalizedY = 0.80f,
        ),
    )
}
