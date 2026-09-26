package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.Alert
import com.hivend.agatha.domain.model.AlertStatus
import com.hivend.agatha.domain.model.HistoryEvent
import com.hivend.agatha.domain.model.AlertLevel
import com.hivend.agatha.domain.model.SensorNode
import com.hivend.agatha.domain.model.AlertTelemetry
import com.hivend.agatha.domain.model.EventType

/**
 * Datos de muestra usados por los repositorios en memoria mientras HE-04..HE-08 no
 * consumen todavía la API central real (ver docs/ARQUITECTURA_Y_DISENO.md § "Datos de
 * ejemplo vs. API real"). El contenido reproduce exactamente los prototipos de Figma
 * (sitio Güepsa – San José de Pare, sensores MP-11xx) para que la demo de navegación se
 * sienta como el diseño aprobado por 0G Colombia.
 */
internal object SampleAgathaData {

    const val SITE = "Güepsa – San José de Pare"

    val alerts: List<Alert> = listOf(
        Alert(
            id = "MP-1156",
            point = "Punto 3",
            sensorId = "MP-1156",
            pk = "PK37+800",
            site = SITE,
            description = "Riesgo de deslizamiento — El tubo se movió 5 cm de su posición original",
            level = AlertLevel.RED,
            status = AlertStatus.IN_INSPECTION,
            relativeTime = "hace 5 min",
            telemetry = AlertTelemetry(
                confidencePercentage = 96,
                acceleration = "Alta",
                relativeTime = "5 min",
                batteryPercentage = 78,
                isRealData = true,
            ),
            nextStep = "Registrar inspección de campo",
        ),
        Alert(
            id = "MP-1189",
            point = "Punto 2",
            sensorId = "MP-1189",
            pk = "PK22+300",
            site = SITE,
            description = "Posible fuga de gas",
            level = AlertLevel.YELLOW,
            status = AlertStatus.RECEIVED,
            relativeTime = "hace 40 min",
            telemetry = AlertTelemetry(
                confidencePercentage = 81,
                acceleration = "Media",
                relativeTime = "40 min",
                batteryPercentage = 64,
                isRealData = true,
            ),
            nextStep = "Iniciar inspección",
        ),
        Alert(
            id = "MP-1122",
            point = "Punto 1",
            sensorId = "MP-1122",
            pk = "PK10+050",
            site = SITE,
            description = "Escenario de prueba — simulación",
            level = AlertLevel.GREEN,
            status = AlertStatus.CLASSIFIED,
            relativeTime = "hace 2 h",
            telemetry = AlertTelemetry(
                confidencePercentage = 100,
                acceleration = "Baja",
                relativeTime = "2 h",
                batteryPercentage = 91,
                isRealData = false,
            ),
            nextStep = "Revisar clasificación",
        ),
        Alert(
            id = "MP-1201",
            point = "Punto 4",
            sensorId = "MP-1201",
            pk = "PK44+120",
            site = SITE,
            description = "Condiciones normales",
            level = AlertLevel.GREEN,
            status = AlertStatus.CLOSED,
            relativeTime = "hace 3 h",
            telemetry = AlertTelemetry(
                confidencePercentage = 98,
                acceleration = "Baja",
                relativeTime = "3 h",
                batteryPercentage = 85,
                isRealData = true,
            ),
            nextStep = "Sin acción pendiente",
        ),
    )

    val nodes: List<SensorNode> = listOf(
        SensorNode(
            id = "MP-1156",
            name = "Nodo MP-1156",
            pk = "PK37+800",
            site = SITE,
            sensorType = "Acelerómetro + giroscopio",
            status = AlertLevel.RED,
            batteryPercentage = 78,
            lastCommunication = "hace 5 min",
            normalizedX = 0.78f,
            normalizedY = 0.22f,
        ),
        SensorNode(
            id = "MP-1189",
            name = "Nodo MP-1189",
            pk = "PK22+300",
            site = SITE,
            sensorType = "Acústico",
            status = AlertLevel.YELLOW,
            batteryPercentage = 64,
            lastCommunication = "hace 40 min",
            normalizedX = 0.45f,
            normalizedY = 0.48f,
        ),
        SensorNode(
            id = "MP-1122",
            name = "Nodo MP-1122",
            pk = "PK10+050",
            site = SITE,
            sensorType = "Condiciones ambientales",
            status = AlertLevel.GREEN,
            batteryPercentage = 91,
            lastCommunication = "hace 2 h",
            normalizedX = 0.20f,
            normalizedY = 0.75f,
        ),
        SensorNode(
            id = "MP-1201",
            name = "Nodo MP-1201",
            pk = "PK44+120",
            site = SITE,
            sensorType = "Energía / batería",
            status = AlertLevel.GREEN,
            batteryPercentage = 85,
            lastCommunication = "hace 3 h",
            normalizedX = 0.83f,
            normalizedY = 0.80f,
        ),
    )

    fun historyFor(deviceId: String): List<HistoryEvent> = when (deviceId) {
        "MP-1156" -> listOf(
            HistoryEvent("12:42", EventType.ALERT, "Alerta roja generada", "Riesgo de deslizamiento — el tubo se movió 5 cm", "Pendiente de confirmar"),
            HistoryEvent("13:05", EventType.INSPECTION, "Inspección registrada", "Resultado: mantenimiento requerido"),
            HistoryEvent("13:07", EventType.CLASSIFICATION, "Clasificación del evento", "Movimiento de tierra o desprendimiento menor"),
            HistoryEvent("13:10", EventType.EVIDENCE, "Evidencia adjunta", "2 fotografías con descripción"),
            HistoryEvent("13:12", EventType.OBSERVATION, "Observación de campo", "Se detecta erosión leve en el talud norte"),
            HistoryEvent("13:20", EventType.MAINTENANCE, "Mantenimiento realizado", "Ajuste de anclaje y limpieza del sensor", "Completado"),
            HistoryEvent("13:25", EventType.ALERT, "Alerta cerrada", "Estado final: Clasificada — sin anomalías adicionales", "Cerrada"),
        )
        else -> listOf(
            HistoryEvent("08:50", EventType.ALERT, "Condiciones normales", "Todos los puntos estables"),
        )
    }
}
