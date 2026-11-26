package com.hivend.agatha.data.local.cache

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.hivend.agatha.data.local.AgathaDatabase
import com.hivend.agatha.data.remote.alerts.AlertDto
import com.hivend.agatha.domain.model.CachedDevices
import com.hivend.agatha.domain.model.TelemetryDevice
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json

/*
 * Caché para abrir la app sin conexión (Sprint 7): lo último que llegó bien del servidor, para
 * mostrarlo con la franja "Sin conexión · datos al …" hasta que vuelva la red. Cada descarga
 * correcta la reemplaza. Las alertas se guardan como el JSON que respondió la API (fila de la
 * lista y detalle por separado) y se vuelven a convertir al leerlas, así la caché no depende de
 * cómo cambie el modelo de dominio.
 */

/** Una alerta guardada: la última fila de lista y el último detalle (el detalle borra la fila vieja). */
@Entity(tableName = "cached_alerts")
data class CachedAlertEntity(
    @PrimaryKey val id: String,
    /** Fila de `GET /api/alerts`, pendientes, activas o historial de un punto; más nueva que [detailJson] si está. */
    val rowJson: String?,
    /** `GET /api/alerts/{id}` con tramos, mensajes y observaciones. */
    val detailJson: String?,
    /** true si salió en la última lista de la bandeja (Sin clasificar + recientes + activas). */
    val inList: Boolean,
)

/** Dispositivo de la API de tablero con su batería y su última conexión. */
@Entity(tableName = "cached_devices")
data class CachedDeviceEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val lastConnection: Instant?,
    val lastBatteryLevel: Double?,
)

/** Cuándo se guardó cada parte de la caché ([KEY_ALERTS], [KEY_DEVICES]). */
@Entity(tableName = "cache_info")
data class CacheInfoEntity(
    @PrimaryKey val key: String,
    val updatedAt: Instant,
)

internal const val KEY_ALERTS = "alerts"
internal const val KEY_DEVICES = "devices"

@Dao
interface OfflineCacheDao {

    @Query("SELECT * FROM cached_alerts")
    suspend fun alerts(): List<CachedAlertEntity>

    @Query("SELECT * FROM cached_alerts WHERE id IN (:ids)")
    suspend fun alertsById(ids: List<String>): List<CachedAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlerts(alerts: List<CachedAlertEntity>)

    @Query("DELETE FROM cached_alerts WHERE inList = 1 AND id NOT IN (:keep)")
    suspend fun deleteListAlertsExcept(keep: List<String>)

    @Query("UPDATE cached_alerts SET inList = 0")
    suspend fun clearListFlags()

    @Query("SELECT * FROM cached_devices ORDER BY name")
    suspend fun devices(): List<CachedDeviceEntity>

    @Query("DELETE FROM cached_devices")
    suspend fun deleteDevices()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<CachedDeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setInfo(info: CacheInfoEntity)

    @Query("SELECT updatedAt FROM cache_info WHERE `key` = :key")
    suspend fun updatedAt(key: String): Instant?

    @Query("SELECT updatedAt FROM cache_info WHERE `key` = :key")
    fun observeUpdatedAt(key: String): Flow<Instant?>

    /** La lista de la bandeja reemplaza a la anterior: las que ya no salen se borran, como en memoria. */
    @Transaction
    suspend fun replaceList(rows: List<CachedAlertEntity>, at: Instant) {
        deleteListAlertsExcept(rows.map { it.id })
        clearListFlags()
        upsertAlerts(rows)
        setInfo(CacheInfoEntity(KEY_ALERTS, at))
    }

    @Transaction
    suspend fun replaceDevices(devices: List<CachedDeviceEntity>, at: Instant) {
        deleteDevices()
        insertDevices(devices)
        setInfo(CacheInfoEntity(KEY_DEVICES, at))
    }
}

/** Una alerta leída de la caché: [row] es más nueva que [detail] cuando están las dos. */
data class CachedAlert(val row: AlertDto?, val detail: AlertDto?, val inList: Boolean)

/** Las alertas que la app guarda para abrir sin conexión. */
interface AlertCache {
    suspend fun loadAlerts(): List<CachedAlert>

    /** Guarda la lista de la bandeja y la hora de la descarga; reemplaza la lista anterior. */
    suspend fun saveList(rows: List<AlertDto>, at: Instant)

    /** Guarda filas sueltas (historial de un punto) sin tocar la lista. */
    suspend fun saveRows(rows: List<AlertDto>)

    suspend fun saveDetail(detail: AlertDto)

    /** Hora de la última lista guardada; null si nunca se descargó. */
    fun observeUpdatedAt(): Flow<Instant?>

    /** Sin caché (pruebas). */
    object None : AlertCache {
        override suspend fun loadAlerts(): List<CachedAlert> = emptyList()
        override suspend fun saveList(rows: List<AlertDto>, at: Instant) = Unit
        override suspend fun saveRows(rows: List<AlertDto>) = Unit
        override suspend fun saveDetail(detail: AlertDto) = Unit
        override fun observeUpdatedAt(): Flow<Instant?> = flowOf(null)
    }
}

/** Los dispositivos de la API de tablero guardados para abrir sin conexión. */
interface DeviceCache {
    suspend fun loadDevices(): CachedDevices?

    /** Reemplaza los dispositivos guardados por los de esta descarga. */
    suspend fun saveDevices(devices: List<TelemetryDevice>, at: Instant)

    object None : DeviceCache {
        override suspend fun loadDevices(): CachedDevices? = null
        override suspend fun saveDevices(devices: List<TelemetryDevice>, at: Instant) = Unit
    }
}

@Singleton
class RoomOfflineCache @Inject constructor(
    private val database: AgathaDatabase,
    private val json: Json,
) : AlertCache, DeviceCache {

    private val dao get() = database.offlineCacheDao()

    override suspend fun loadAlerts(): List<CachedAlert> = dao.alerts().map {
        CachedAlert(row = decode(it.rowJson), detail = decode(it.detailJson), inList = it.inList)
    }

    override suspend fun saveList(rows: List<AlertDto>, at: Instant) {
        val known = dao.alertsById(rows.map { it.code }).associateBy { it.id }
        dao.replaceList(rows.map { row -> rowEntity(row, known[row.code], inList = true) }, at)
    }

    override suspend fun saveRows(rows: List<AlertDto>) {
        val known = dao.alertsById(rows.map { it.code }).associateBy { it.id }
        dao.upsertAlerts(rows.map { row -> rowEntity(row, known[row.code], inList = known[row.code]?.inList == true) })
    }

    override suspend fun saveDetail(detail: AlertDto) {
        val known = dao.alertsById(listOf(detail.code)).firstOrNull()
        dao.upsertAlerts(
            listOf(
                CachedAlertEntity(
                    id = detail.code,
                    rowJson = null,
                    detailJson = json.encodeToString(AlertDto.serializer(), detail),
                    inList = known?.inList == true,
                ),
            ),
        )
    }

    override fun observeUpdatedAt(): Flow<Instant?> = dao.observeUpdatedAt(KEY_ALERTS)

    private fun rowEntity(row: AlertDto, known: CachedAlertEntity?, inList: Boolean) = CachedAlertEntity(
        id = row.code,
        rowJson = json.encodeToString(AlertDto.serializer(), row),
        detailJson = known?.detailJson,
        inList = inList,
    )

    private fun decode(text: String?): AlertDto? =
        text?.let { runCatching { json.decodeFromString(AlertDto.serializer(), it) }.getOrNull() }

    // ---- Dispositivos

    override suspend fun loadDevices(): CachedDevices? {
        val at = dao.updatedAt(KEY_DEVICES) ?: return null
        val devices = dao.devices().map { TelemetryDevice(it.id, it.name, it.lastConnection, it.lastBatteryLevel) }
        return CachedDevices(devices, at)
    }

    override suspend fun saveDevices(devices: List<TelemetryDevice>, at: Instant) =
        dao.replaceDevices(devices.map { CachedDeviceEntity(it.id, it.name, it.lastConnection, it.lastBatteryLevel) }, at)
}
