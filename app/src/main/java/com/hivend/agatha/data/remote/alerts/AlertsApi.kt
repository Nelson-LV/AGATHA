package com.hivend.agatha.data.remote.alerts

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * API de gestión de alertas del backend de AGATHA (la misma de la web, sin autenticación).
 * Fechas de respuesta en RFC 3339, hora de Colombia. `{id}` es el id numérico o el código
 * ("ALR-000123") de la **alerta**, nunca el del dispositivo. Errores: `{code, message}` con el
 * mensaje en español, listo para mostrar. Los envíos de la app llevan `origin = "APP_MOVIL"`.
 */
interface AlertsApi {

    /**
     * Una página (10) de alertas, la más reciente primero. [device] = `measuring_point_id`. Los
     * demás filtros son los del Historial (ver [AlertSearchQuery]); null = no se filtra por ese.
     */
    @GET("api/alerts")
    suspend fun alerts(
        @Query("device") device: String? = null,
        @Query("page") page: Int? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("max_level") maxLevel: String? = null,
        @Query("event") event: String? = null,
        @Query("status") status: String? = null,
    ): AlertPageDto

    /**
     * Excel (.xlsx) con todas las alertas que cumplen los mismos filtros de [alerts], sin página.
     * El cuerpo es binario; si falla, el cuerpo de error es el `{code, message}` de siempre.
     */
    @Streaming
    @GET("api/alerts/export.xlsx")
    suspend fun export(
        @Query("device") device: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("max_level") maxLevel: String? = null,
        @Query("event") event: String? = null,
        @Query("status") status: String? = null,
    ): Response<ResponseBody>

    /**
     * Alertas Sin clasificar, la más reciente primero (guía §5.2). Sin [limit] el backend trae
     * solo 5; `total` cuenta todas las pendientes aunque `items` traiga como máximo [limit].
     */
    @GET("api/alerts/pending")
    suspend fun pending(@Query("limit") limit: Int? = PENDING_LIMIT): AlertPendingDto

    companion object {
        /** Máximo que acepta `GET /api/alerts/pending`. */
        const val PENDING_LIMIT = 50
    }

    @GET("api/alerts/{id}")
    suspend fun alert(@Path("id") id: String): AlertDto

    // Los envíos devuelven el cuerpo crudo: el éxito se decide por el código HTTP, y el cuerpo
    // (ver [ActionBodyDto]) solo se lee para los avisos; así un cambio en él no pierde un envío.

    /** 201 SAVED, 200 ALREADY_RECEIVED o 409 SAVED_AS_CONFLICT_OBSERVATION (cuerpo en errorBody), todos con `outcome` en el cuerpo. */
    @POST("api/alerts/{id}/classification")
    suspend fun classify(@Path("id") id: String, @Body body: ClassificationRequestDto): Response<ResponseBody>

    /** 409 CONFLICT si la alerta aún no está clasificada. */
    @POST("api/alerts/{id}/label")
    suspend fun label(@Path("id") id: String, @Body body: LabelRequestDto): Response<ResponseBody>

    @POST("api/alerts/{id}/observations")
    suspend fun observe(@Path("id") id: String, @Body body: ObservationRequestDto): Response<ResponseBody>
}

@Serializable
data class AlertPageDto(
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
    val items: List<AlertDto> = emptyList(),
)

@Serializable
data class AlertPendingDto(
    val total: Int = 0,
    val items: List<AlertDto> = emptyList(),
)

@Serializable
data class DeviceRefDto(
    val id: Int,
    /** Nombre del dispositivo ("Sensor PK34-470"): se muestra tal cual. */
    val pk: String? = null,
)

/** Fila de la lista y detalle de una alerta: los campos de detalle solo vienen en `GET /{id}`. */
@Serializable
data class AlertDto(
    val id: Int,
    val code: String,
    val device: DeviceRefDto,
    val event: String? = null,
    val state: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("in_progress") val inProgress: Boolean = false,
    @SerialName("duration_hours") val durationHours: Double? = null,
    @SerialName("current_level") val currentLevel: String? = null,
    @SerialName("max_level") val maxLevel: String? = null,
    val blinking: Boolean? = null,
    val classification: String? = null,
    @SerialName("management_status") val managementStatus: String? = null,
    val label: String? = null,
    @SerialName("label_other_text") val labelOtherText: String? = null,
    @SerialName("available_actions") val availableActions: List<String>? = null,
    // ---- solo en el detalle ----
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("reactivation_count") val reactivationCount: Int? = null,
    @SerialName("last_reactivated_at") val lastReactivatedAt: String? = null,
    @SerialName("opening_indicator_value") val openingIndicatorValue: Double? = null,
    @SerialName("last_indicator_value") val lastIndicatorValue: Double? = null,
    @SerialName("max_indicator_value") val maxIndicatorValue: Double? = null,
    @SerialName("last_evaluated_at") val lastEvaluatedAt: String? = null,
    @SerialName("classification_detail") val classificationDetail: ClassificationDetailDto? = null,
    @SerialName("label_detail") val labelDetail: LabelDetailDto? = null,
    val observations: List<ObservationDto> = emptyList(),
    val levels: List<LevelDto> = emptyList(),
)

@Serializable
data class ClassificationDetailDto(
    val value: String? = null,
    val at: String? = null,
    val origin: String? = null,
    @SerialName("device_identifier") val deviceIdentifier: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
)

@Serializable
data class LabelDetailDto(
    val value: String? = null,
    @SerialName("other_text") val otherText: String? = null,
    val at: String? = null,
    val origin: String? = null,
    @SerialName("device_identifier") val deviceIdentifier: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
)

@Serializable
data class ObservationDto(
    val id: Long,
    val text: String = "",
    val kind: String? = null,
    val origin: String? = null,
    @SerialName("device_identifier") val deviceIdentifier: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null,
    @SerialName("received_at") val receivedAt: String? = null,
)

/** Tramo de un color de la alerta (guía §5.4). */
@Serializable
data class LevelDto(
    val seq: Int = 0,
    @SerialName("opened_by") val openedBy: String? = null,
    val level: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    val messages: List<LevelMessageDto> = emptyList(),
)

/** Mensaje de un tramo: [title] y [body] vienen redactados por el backend y se muestran tal cual. */
@Serializable
data class LevelMessageDto(
    val id: Long = 0,
    val at: String? = null,
    val kind: String? = null,
    val code: String? = null,
    val level: String? = null,
    @SerialName("indicator_value") val indicatorValue: Double? = null,
    val title: String? = null,
    val body: String? = null,
)

// Los cuerpos de envío no llevan valores por defecto: kotlinx.serialization los omitiría y el
// backend es estricto (campo desconocido → 400).

@Serializable
data class ClassificationRequestDto(
    val classification: String,
    val observation: String?,
    val origin: String,
    @SerialName("device_identifier") val deviceIdentifier: String,
    @SerialName("client_record_id") val clientRecordId: String,
    @SerialName("recorded_at") val recordedAt: String?,
)

@Serializable
data class LabelRequestDto(
    val label: String,
    @SerialName("other_text") val otherText: String?,
    val observation: String?,
    val origin: String,
    @SerialName("device_identifier") val deviceIdentifier: String,
    @SerialName("client_record_id") val clientRecordId: String,
    @SerialName("recorded_at") val recordedAt: String?,
)

/**
 * Observación (guía §7.4). Con [clientRecordId] el servidor descarta un reintento del mismo
 * registro (responde 200 ALREADY_RECEIVED), así una observación de la cola no se duplica.
 */
@Serializable
data class ObservationRequestDto(
    val text: String,
    val origin: String,
    @SerialName("device_identifier") val deviceIdentifier: String,
    @SerialName("client_record_id") val clientRecordId: String,
    @SerialName("recorded_at") val recordedAt: String?,
)

/**
 * Cuerpo de las respuestas de los envíos, de éxito o de error. La app no depende del resto: tras
 * un envío vuelve a pedir el detalle de la alerta.
 */
@Serializable
data class ActionBodyDto(
    val outcome: String? = null,
    val code: String? = null,
    val message: String? = null,
    val existing: ExistingClassificationDto? = null,
)

@Serializable
data class ExistingClassificationDto(
    val classification: String? = null,
    @SerialName("classified_at") val classifiedAt: String? = null,
    val origin: String? = null,
    @SerialName("device_identifier") val deviceIdentifier: String? = null,
)
