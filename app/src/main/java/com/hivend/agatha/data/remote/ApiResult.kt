package com.hivend.agatha.data.remote

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Resultado de una llamada a la API del backend: el valor o un [ApiError] ya clasificado. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/**
 * Por qué falló una llamada. Los repositorios lo traducen a su propio error de dominio y la cola
 * de sincronización decide con [isRetryable] si reintenta (RN-17) o deja el registro en Error.
 */
sealed interface ApiError {
    /** Sin internet, tiempo agotado o el servidor no aceptó la conexión. */
    data object NoConnection : ApiError

    /** El servidor respondió [code]; [body] es su `{code, message}` si lo mandó en ese formato. */
    data class Http(val code: Int, val body: ApiErrorBody?) : ApiError

    /** La respuesta llegó pero no corresponde al DTO esperado. */
    data object InvalidResponse : ApiError
}

/** Formato de error del backend de AGATHA, p. ej. `{"code": "NOT_FOUND", "message": "…"}`. */
@Serializable
data class ApiErrorBody(val code: String, val message: String)

/**
 * true si vale la pena reintentar más tarde: sin conexión, 408, 429 o 5xx. Un 4xx distinto es
 * un rechazo del servidor que no cambia al reenviar lo mismo.
 */
val ApiError.isRetryable: Boolean
    get() = when (this) {
        ApiError.NoConnection -> true
        is ApiError.Http -> code == 408 || code == 429 || code >= 500
        ApiError.InvalidResponse -> false
    }

/** 409: otro cliente cambió el registro antes (RN-18, p. ej. la alerta ya estaba clasificada). */
val ApiError.isConflict: Boolean
    get() = this is ApiError.Http && code == 409

/**
 * Ejecuta una llamada de Retrofit y convierte sus excepciones en [ApiResult]. La cancelación de
 * la corrutina se propaga; [json] lee el cuerpo de error del backend.
 */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        ApiResult.Failure(ApiError.NoConnection)
    } catch (e: HttpException) {
        ApiResult.Failure(ApiError.Http(e.code(), e.errorBody(json)))
    } catch (_: SerializationException) {
        ApiResult.Failure(ApiError.InvalidResponse)
    }

private fun HttpException.errorBody(json: Json): ApiErrorBody? =
    try {
        response()?.errorBody()?.string()?.let { json.decodeFromString<ApiErrorBody>(it) }
    } catch (_: SerializationException) {
        null
    } catch (_: IOException) {
        null
    }
