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

/**
 * API de gestión de alertas del backend de AGATHA (la misma de la web, sin autenticación).
 * Fechas de respuesta en RFC 3339, hora de Colombia. `{id}` es el id numérico o el código
 * ("ALR-000123") de la **alerta**, nunca el del dispositivo. Errores: `{code, message}` con el
 * mensaje en español, listo para mostrar. Los envíos de la app llevan `origin = "APP_MOVIL"`.
 */
interface AlertsApi {

    /** Una página (10) de alertas, la más reciente primero. [device] = `measuring_point_id`. */
    @GET("api/alerts")
    suspend fun alerts(
        @Query("device") device: String? = null,
        @Query("page") page: Int? = null,
    ): AlertPageDto

    /** Alertas Sin clasificar. */
    @GET("api/alerts/pending")
    suspend fun pending(): AlertPendingDto

    @GET("api/alerts/{id}")
    suspend fun alert(@Path("id") id: String): AlertDto

    // Los envíos devuelven el cuerpo crudo: el éxito se decide por el código HTTP, y el cuerpo
    // (ver [ActionBodyDto]) solo se lee para los avisos; así un cambio en él no pierde un envío.

    /** 201 SAVED, 200 ALREADY_RECEIVED o 409 SAVED_AS_CONFLICT_OBSERVATION (cuerpo en errorBody). */
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
    val pk: String = "",
)

/** Fila de la lista y detalle de una alerta: los campos de detalle solo vienen en `GET /{id}`. */
@Serializable
data class AlertDto(
    val id: Int,
    val code: String,
    val device: DeviceRefDto,
    val event: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("in_progress") val inProgress: Boolean = false,
    @SerialName("current_level") val currentLevel: String? = null,
    @SerialName("max_level") val maxLevel: String? = null,
    val classification: String? = null,
    val label: String? = null,
    @SerialName("label_other_text") val labelOtherText: String? = null,
    // ---- solo en el detalle ----
    @SerialName("opened_at") val openedAt: String? = null,
    @SerialName("last_indicator_value") val lastIndicatorValue: Double? = null,
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

@Serializable
data class LevelDto(
    val seq: Int = 0,
    val level: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    val messages: List<LevelMessageDto> = emptyList(),
)

@Serializable
data class LevelMessageDto(
    val id: Long = 0,
    val at: String? = null,
    @SerialName("indicator_value") val indicatorValue: Double? = null,
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

@Serializable
data class ObservationRequestDto(
    val text: String,
    val origin: String,
    @SerialName("device_identifier") val deviceIdentifier: String?,
)

/**
 * Cuerpo de las respuestas de los envíos, de éxito o de error. La app no depende del resto: tras
 * un envío vuelve a pedir el detalle de la alerta.
 */
@Serializable
data class ActionBodyDto(
    val status: String? = null,
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
