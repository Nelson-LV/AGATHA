package com.hivend.agatha.data.sync

import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.apiCall
import com.hivend.agatha.data.remote.devices.DevicesApi
import com.hivend.agatha.data.remote.devices.PushTokenRequestDto
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.push.PushTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.serialization.json.Json

/**
 * Envía el token FCM vigente del celular (PUT /api/devices/push-token). La cola no reintenta un
 * 4xx ([SyncQueueProcessor]); el siguiente intento es el del próximo arranque de la app.
 */
class PushTokenSender @Inject constructor(
    private val api: DevicesApi,
    private val tokens: PushTokenStore,
    private val device: DeviceOriginProvider,
    private val json: Json,
) : SyncRecordSender {
    override val type = SyncRecordType.PUSH_TOKEN

    override suspend fun send(recordId: String): ApiResult<Unit> {
        // FCM dio de baja el token mientras esperaba en la cola: no hay nada que registrar.
        val token = tokens.token ?: return ApiResult.Success(Unit)
        return apiCall(json) { api.registerPushToken(PushTokenRequestDto(device.deviceId, token)) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PushSyncModule {

    @Binds
    @IntoSet
    abstract fun bindPushTokenSender(impl: PushTokenSender): SyncRecordSender
}
