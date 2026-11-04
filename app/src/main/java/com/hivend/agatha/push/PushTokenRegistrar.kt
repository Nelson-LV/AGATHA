package com.hivend.agatha.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.hivend.agatha.BuildConfig
import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.data.local.sync.SyncQueueDao
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.data.sync.SyncScheduler
import com.hivend.agatha.domain.model.SyncRecordType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Registra el token FCM en el backend en cada arranque y cada vez que FCM lo renueva. El envío
 * sale por la cola de sincronización ([com.hivend.agatha.data.sync.PushTokenSender]): sin red
 * espera, un 5xx se reintenta y un 4xx no; queda en Error hasta el próximo arranque.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val tokenStore: PushTokenStore,
    private val queue: SyncQueueDao,
    private val scheduler: SyncScheduler,
    private val deviceOrigin: DeviceOriginProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerOnStartup() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            // Sin Play Services o sin red no hay token nuevo: se registra el guardado, si lo hay.
            if (task.isSuccessful) tokenStore.save(task.result) else Log.w(TAG, "FCM token unavailable", task.exception)
            logForDebugging()
            enqueueRegistration()
        }
    }

    fun onNewToken(token: String) {
        tokenStore.save(token)
        logForDebugging()
        enqueueRegistration()
    }

    private fun enqueueRegistration() {
        if (tokenStore.token == null) return
        scope.launch {
            // Una sola fila: cada registro reemplaza al anterior, aunque hubiera quedado en Error.
            queue.enqueueAgain(
                SyncQueueEntity(
                    id = QUEUE_ENTRY_ID,
                    recordType = SyncRecordType.PUSH_TOKEN,
                    recordId = QUEUE_ENTRY_ID,
                    createdAt = Instant.now(),
                ),
            )
            scheduler.syncNow()
        }
    }

    /** Solo en debug: el backend pide token y deviceId del celular de prueba desde el log. */
    private fun logForDebugging() {
        if (BuildConfig.DEBUG) Log.d(TAG, "deviceId=${deviceOrigin.deviceId} fcmToken=${tokenStore.token}")
    }

    private companion object {
        const val TAG = "AgathaPush"
        const val QUEUE_ENTRY_ID = "push-token"
    }
}
