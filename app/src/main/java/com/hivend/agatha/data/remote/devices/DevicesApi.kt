package com.hivend.agatha.data.remote.devices

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.PUT

/** Registro del celular para recibir push (docs/CONTRATO_PUSH_FCM.md § 1). */
interface DevicesApi {

    @PUT("api/devices/push-token")
    suspend fun registerPushToken(@Body request: PushTokenRequestDto)
}

@Serializable
data class PushTokenRequestDto(
    val deviceId: String,
    val token: String,
    val platform: String = "ANDROID",
)
