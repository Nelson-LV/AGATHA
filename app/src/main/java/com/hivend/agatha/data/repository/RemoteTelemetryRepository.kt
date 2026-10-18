package com.hivend.agatha.data.repository

import com.hivend.agatha.data.remote.dashboard.DashboardApi
import com.hivend.agatha.data.remote.dashboard.toDomain
import com.hivend.agatha.domain.model.DeviceTelemetry
import com.hivend.agatha.domain.model.TelemetryDevice
import com.hivend.agatha.domain.model.TelemetryError
import com.hivend.agatha.domain.model.TelemetryResult
import com.hivend.agatha.domain.repository.TelemetryRepository
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

/**
 * Telemetría leída de la API de tablero. Relaciona el nodo de la app con el dispositivo de la
 * plataforma por su código en el nombre ("MP-11556" en "Sensor MP-11556 PK34-470"), porque la
 * API identifica los dispositivos con un id numérico propio.
 */
@Singleton
class RemoteTelemetryRepository @Inject constructor(
    private val api: DashboardApi,
) : TelemetryRepository {

    /** Reloj inyectable para las pruebas. */
    internal var now: () -> Instant = Instant::now

    private val devicesLock = Mutex()
    private var cachedDevices: List<TelemetryDevice>? = null
    private var cachedAt: Instant = Instant.EPOCH

    override suspend fun findDevice(nodeId: String): TelemetryResult<TelemetryDevice> =
        call {
            val device = devices().firstOrNull { it.matchesNode(nodeId) }
            if (device == null) TelemetryResult.Failure(TelemetryError.DEVICE_NOT_FOUND) else TelemetryResult.Success(device)
        }

    override suspend fun recentTelemetry(
        nodeId: String,
        keys: Set<String>,
        window: Duration,
    ): TelemetryResult<DeviceTelemetry> = call {
        val device = devices().firstOrNull { it.matchesNode(nodeId) }
            ?: return@call TelemetryResult.Failure(TelemetryError.DEVICE_NOT_FOUND)
        // Un minuto de margen: la API rechaza un fin en el futuro (FUTURE_DATE) y los relojes
        // del celular y del servidor no siempre coinciden.
        val to = now().truncatedTo(ChronoUnit.SECONDS).minus(CLOCK_MARGIN)
        val from = to.minus(window)
        val response = api.series(
            deviceId = device.id,
            startDate = from.toString(),
            endDate = to.toString(),
            variables = keys.takeIf { it.isNotEmpty() }?.joinToString(","),
        )
        TelemetryResult.Success(
            DeviceTelemetry(
                device = device,
                from = from,
                to = to,
                series = response.series.map { it.toDomain() },
            ),
        )
    }

    private suspend fun devices(): List<TelemetryDevice> = devicesLock.withLock {
        val cached = cachedDevices
        if (cached != null && Duration.between(cachedAt, now()) < DEVICES_TTL) return cached
        api.devices().map { it.toDomain() }.also {
            cachedDevices = it
            cachedAt = now()
        }
    }

    private inline fun <T> call(block: () -> TelemetryResult<T>): TelemetryResult<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            TelemetryResult.Failure(TelemetryError.NO_CONNECTION)
        } catch (e: HttpException) {
            TelemetryResult.Failure(if (e.code() == 404) TelemetryError.DEVICE_NOT_FOUND else TelemetryError.SERVER)
        } catch (_: SerializationException) {
            TelemetryResult.Failure(TelemetryError.SERVER)
        }

    private companion object {
        val CLOCK_MARGIN: Duration = Duration.ofMinutes(1)

        /** La lista de dispositivos cambia poco; las series sí se piden cada vez. */
        val DEVICES_TTL: Duration = Duration.ofMinutes(5)
    }
}
