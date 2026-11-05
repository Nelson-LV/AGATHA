package com.hivend.agatha.data.remote.devices

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PUT

/** Valor de `platform` en el registro: el backend responde 400 si falta o es otro. */
const val PUSH_PLATFORM_ANDROID = "ANDROID"

/** Registro del celular para recibir push (docs/CONTRATO_PUSH_FCM.md § 1). */
interface DevicesApi {

    @PUT("api/devices/push-token")
    suspend fun registerPushToken(@Body request: PushTokenRequestDto)
}

/** Sin valores por defecto: el `Json` de la app no codifica los defaults y el campo no viajaría. */
@Serializable
data class PushTokenRequestDto(
    val deviceId: String,
    val token: String,
    val platform: String,
)
