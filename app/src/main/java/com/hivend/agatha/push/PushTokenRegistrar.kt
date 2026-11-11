package com.hivend.agatha.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.hivend.agatha.BuildConfig
import com.hivend.agatha.core.device.DeviceOriginProvider
import com.hivend.agatha.data.sync.PushTokenScheduler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra el token FCM en el backend en cada arranque y cada vez que FCM lo renueva. El envío y
 * sus reintentos los hace [com.hivend.agatha.data.sync.PushTokenWorker].
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val tokenStore: PushTokenStore,
    private val scheduler: PushTokenScheduler,
    private val deviceOrigin: DeviceOriginProvider,
) {
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
        if (tokenStore.token != null) scheduler.register()
    }

    /** Solo en debug: el backend pide token y deviceId del celular de prueba desde el log. */
    private fun logForDebugging() {
        if (BuildConfig.DEBUG) Log.d(TAG, "deviceId=${deviceOrigin.deviceId} fcmToken=${tokenStore.token}")
    }

    private companion object {
        const val TAG = "AgathaPush"
    }
}
