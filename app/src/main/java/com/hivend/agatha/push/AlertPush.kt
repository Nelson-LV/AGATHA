package com.hivend.agatha.push

import com.hivend.agatha.data.remote.dashboard.parseRfc3339
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertEvent
import com.hivend.agatha.domain.model.AlertLevel
import java.time.Instant

/** Motivo del push (HU-4.1): la alerta se acaba de generar o subió de nivel. */
enum class AlertPushKind {
    NEW,
    LEVEL_UP,
}

/**
 * Lo que la app necesita para avisar de una alerta (HU-4.1). Llega como mensaje FCM de
 * **solo datos**: el texto lo arma la app con sus recursos para seguir el idioma del
 * dispositivo, nunca se muestra texto redactado por el servidor.
 */
data class AlertPush(
    val alertId: String,
    val level: AlertLevel,
    val kind: AlertPushKind,
    val sensorId: String? = null,
    val pk: String? = null,
    val event: AlertEvent? = null,
    val startedAt: Instant? = null,
)

/**
 * Convierte el `data` de un mensaje FCM en un [AlertPush], o null si no se debe avisar.
 *
 * Contrato propuesto al equipo web (los nombres pueden cambiar; los campos extra se ignoran):
 * `alertId`, `level` (YELLOW/ORANGE/RED), `kind` (NEW/LEVEL_UP), `sensorId`, `pk`, `event`
 * (LEAK/MOVEMENT/LEAK_AND_MOVEMENT), `startedAt` (RFC 3339) y `classification`.
 *
 * Nunca hay push por un aviso gris (RN-02), por el nivel normal ni por una alerta clasificada
 * como Falsa alerta (HU-4.1): esos mensajes, o los que no traen id o nivel válidos, se descartan.
 */
object AlertPushParser {

    fun parse(data: Map<String, String>): AlertPush? {
        val alertId = data[KEY_ALERT_ID]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val level = data[KEY_LEVEL].toEnumOrNull<AlertLevel>()?.takeIf { it.isAlert } ?: return null
        if (data[KEY_CLASSIFICATION].toEnumOrNull<AlertClassification>() == AlertClassification.FALSE_ALARM) return null
        return AlertPush(
            alertId = alertId,
            level = level,
            kind = data[KEY_KIND].toEnumOrNull<AlertPushKind>() ?: AlertPushKind.NEW,
            sensorId = data[KEY_SENSOR_ID].nonBlank(),
            pk = data[KEY_PK].nonBlank(),
            event = data[KEY_EVENT].toEnumOrNull<AlertEvent>(),
            startedAt = parseRfc3339(data[KEY_STARTED_AT].nonBlank()),
        )
    }

    private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? =
        this?.trim()?.uppercase()?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    private fun String?.nonBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private const val KEY_ALERT_ID = "alertId"
    private const val KEY_LEVEL = "level"
    private const val KEY_KIND = "kind"
    private const val KEY_SENSOR_ID = "sensorId"
    private const val KEY_PK = "pk"
    private const val KEY_EVENT = "event"
    private const val KEY_STARTED_AT = "startedAt"
    private const val KEY_CLASSIFICATION = "classification"
}
