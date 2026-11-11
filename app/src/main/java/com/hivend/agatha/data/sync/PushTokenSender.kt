package com.hivend.agatha.data.sync

import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.apiCall
import com.hivend.agatha.data.remote.devices.DevicesApi
import com.hivend.agatha.data.remote.devices.PUSH_PLATFORM_ANDROID
import com.hivend.agatha.data.remote.devices.PushTokenRequestDto
import com.hivend.agatha.push.PushTokenStore
import javax.inject.Inject
import kotlinx.serialization.json.Json

/** Envía el token FCM vigente del celular (PUT /api/devices/push-token); lo llama [PushTokenWorker]. */
class PushTokenSender @Inject constructor(
    private val api: DevicesApi,
    private val tokens: PushTokenStore,
    private val device: DeviceOriginProvider,
    private val json: Json,
) {
    suspend fun send(): ApiResult<Unit> {
        // FCM dio de baja el token mientras esperaba su turno: no hay nada que registrar.
        val token = tokens.token ?: return ApiResult.Success(Unit)
        return apiCall(json) { api.registerPushToken(PushTokenRequestDto(device.deviceId, token, PUSH_PLATFORM_ANDROID)) }
    }
}
