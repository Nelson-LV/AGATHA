package com.hivend.agatha.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Dispositivo tal como lo reporta la API de tablero del backend (`/api/dashboard/devices`).
 * [lastConnection] es la hora del último dato recibido, no un estado "en línea": el nodo
 * envía lecturas cada ~15 min, así que la conexión se juzga comparando con la hora actual.
 */
data class TelemetryDevice(
    val id: Int,
    /** Nombre en la plataforma, p. ej. "Sensor MP-11556 PK34-470". */
    val name: String,
    val lastConnection: Instant?,
    /** Último nivel de batería informado, en %. */
    val lastBatteryLevel: Double?,
) {
    /** Código del nodo en la app: el "MP-xxxx" del nombre o, si no lo trae, "MP-" + [id]. */
    val nodeCode: String
        get() = CODE.find(name)?.value?.uppercase() ?: "MP-$id"

    /** PK del nombre en el formato de la web ("PK34-470" → "PK34+470"); null si no lo trae. */
    val pk: String?
        get() = PK.find(name)?.let { "PK${it.groupValues[1]}+${it.groupValues[2]}" }

    /**
     * true si el último dato llegó hace menos de [CONNECTED_WINDOW]. El nodo envía una lectura
     * cada ~15 min, así que se toleran dos lecturas perdidas antes de darlo por desconectado.
     */
    fun isConnected(now: Instant): Boolean =
        lastConnection?.let { Duration.between(it, now) < CONNECTED_WINDOW } ?: false

    /**
     * true si este dispositivo es el nodo [nodeId] de la app ("MP-11556"): por el código en
     * [name] ("Sensor MP-11556 PK34-470") o, si el nombre no lo trae ("Sensor 11556", como en
     * la documentación del endpoint), porque el número del código es el [id].
     */
    fun matchesNode(nodeId: String): Boolean {
        val byName = Regex("(^|[^A-Za-z0-9])${Regex.escape(nodeId)}([^A-Za-z0-9]|$)", RegexOption.IGNORE_CASE)
            .containsMatchIn(name)
        return byName || Regex("\\d+$").find(nodeId)?.value?.toIntOrNull() == id
    }

    private companion object {
        val CODE = Regex("MP-\\d+", RegexOption.IGNORE_CASE)
        val PK = Regex("PK\\s?(\\d+)\\s?[-+]\\s?(\\d+)", RegexOption.IGNORE_CASE)
        val CONNECTED_WINDOW: Duration = Duration.ofMinutes(45)
    }
}

/** De dónde sale la serie: lectura directa del sensor o cálculo del backend. */
enum class TelemetrySource {
    MEASUREMENT,
    EVALUATION,
}

data class TelemetryPoint(
    val at: Instant,
    val value: Double,
)

/**
 * Una variable de telemetría en un rango de tiempo. Los huecos son reales (el nodo no envió
 * datos): no se rellenan, ver [segments].
 */
data class TelemetrySeries(
    /** Clave del catálogo del backend ("temperatura", "b1", "indicador", …). */
    val key: String,
    /** Nombre que da el backend; la app usa su propio texto traducido si conoce la clave. */
    val name: String,
    val unit: String,
    val source: TelemetrySource,
    val points: List<TelemetryPoint>,
) {
    /**
     * Tramos continuos de la serie. Se corta donde dos puntos seguidos están separados más de
     * [GAP_FACTOR] veces el intervalo típico (la mediana), para que la gráfica no una con una
     * recta un periodo sin datos.
     */
    fun segments(): List<List<TelemetryPoint>> {
        val sorted = points.sortedBy { it.at }
        if (sorted.size < 2) return if (sorted.isEmpty()) emptyList() else listOf(sorted)
        val steps = sorted.zipWithNext { a, b -> Duration.between(a.at, b.at).toMillis() }
        val typical = steps.sorted()[steps.size / 2].coerceAtLeast(1)
        val result = mutableListOf(mutableListOf(sorted.first()))
        sorted.zipWithNext().forEachIndexed { i, (_, next) ->
            if (steps[i] > typical * GAP_FACTOR) result += mutableListOf<TelemetryPoint>()
            result.last() += next
        }
        return result
    }

    companion object {
        const val GAP_FACTOR = 2.5
    }
}

/** Series de un dispositivo en el rango [from, to). */
data class DeviceTelemetry(
    val device: TelemetryDevice,
    val from: Instant,
    val to: Instant,
    val series: List<TelemetrySeries>,
) {
    fun series(key: String): TelemetrySeries? = series.firstOrNull { it.key == key }
}

/**
 * Sección de sensores de la pestaña Dispositivos. Agrupa las variables del backend como lo
 * pide la API (las secciones las arma el cliente). Cada elemento de [charts] es una gráfica;
 * dos claves en la misma lista comparten eje (pitch y roll). Indicador y densidad van en
 * gráficas separadas porque sus escalas no se comparan.
 */
enum class SensorSection(val charts: List<List<String>>) {
    MOTION(listOf(listOf("deriva"), listOf("pitch", "roll"), listOf("aceleracion"))),
    LEAK(listOf(listOf("b1", "b2"), listOf("indicador"), listOf("densidad"))),
    ENVIRONMENT(listOf(listOf("temperatura"), listOf("humedad"))),

    /** Sin series: muestra la última batería y la última conexión del dispositivo. */
    POWER(emptyList()),
    ;

    val keys: Set<String> get() = charts.flatten().toSet()

    companion object {
        /** Clave del indicador de fuga: la gráfica de "Ver gráfica" en el detalle (HU-4.2). */
        const val INDICATOR_KEY = "indicador"
    }
}

/** Por qué no se pudo traer la telemetría; la UI lo traduce a un mensaje. */
enum class TelemetryError {
    /** Sin internet o el servidor no responde: las gráficas solo se ven con conexión. */
    NO_CONNECTION,

    /** La plataforma no tiene un dispositivo con el id de este nodo. */
    DEVICE_NOT_FOUND,

    /** La API respondió con error (400/404/500). */
    SERVER,
}

sealed interface TelemetryResult<out T> {
    data class Success<T>(val data: T) : TelemetryResult<T>
    data class Failure(val error: TelemetryError) : TelemetryResult<Nothing>
}
