package com.hivend.agatha.data.remote.dashboard

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API de tablero del backend de AGATHA (Go, solo lectura, sin autenticación). Fechas en
 * RFC 3339; el rango de `series` es [startDate, endDate), con start < end y ninguna futura.
 * Errores: `{code, message}` con 400 INVALID_INPUT / FUTURE_DATE, 404 NOT_FOUND y 500.
 */
interface DashboardApi {

    /** Todos los dispositivos, activos o no, sin orden garantizado. */
    @GET("api/dashboard/devices")
    suspend fun devices(): List<DeviceDto>

    /** Catálogo de variables: clave, nombre, unidad y origen. */
    @GET("api/dashboard/variables")
    suspend fun variables(): List<VariableDto>

    /** Series de un dispositivo; [variables] separadas por coma, null = todas. */
    @GET("api/dashboard/series")
    suspend fun series(
        @Query("deviceId") deviceId: Int,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("variables") variables: String? = null,
    ): SeriesResponseDto
}

@Serializable
data class DeviceDto(
    val id: Int,
    val name: String,
    val lastConnection: String? = null,
    val lastBatteryLevel: Double? = null,
)

@Serializable
data class VariableDto(
    val key: String,
    val name: String,
    val unit: String,
    val source: String,
)

@Serializable
data class SeriesResponseDto(
    val deviceId: Int,
    val startDate: String,
    val endDate: String,
    val series: List<SeriesDto> = emptyList(),
)

@Serializable
data class SeriesDto(
    val key: String,
    val name: String,
    val unit: String,
    val source: String,
    val data: List<PointDto> = emptyList(),
)

@Serializable
data class PointDto(
    val timestamp: String,
    val value: Double? = null,
)
