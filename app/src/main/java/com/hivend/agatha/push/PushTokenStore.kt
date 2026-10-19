package com.hivend.agatha.push

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Último token FCM del celular. El backend lo necesita junto al id del dispositivo
 * ([com.hivend.agatha.core.device.DeviceOriginProvider]) para mandarle los push.
 *
 * Pendiente: la API todavía no tiene el endpoint de registro de tokens. Cuando exista, el
 * motor de sincronización (data/sync, HE-07) enviará el token mientras [needsUpload] sea true
 * y llamará a [markUploaded]; no se agrega un mecanismo de reintentos aparte.
 */
@Singleton
class PushTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)

    val needsUpload: Boolean get() = token != null && !prefs.getBoolean(KEY_UPLOADED, false)

    fun save(token: String) {
        if (token == this.token) return
        prefs.edit {
            putString(KEY_TOKEN, token)
            putBoolean(KEY_UPLOADED, false)
        }
    }

    fun markUploaded() = prefs.edit { putBoolean(KEY_UPLOADED, true) }

    /** FCM dio de baja [token]: se olvida si era el guardado. */
    fun clear(token: String) {
        if (token == this.token) prefs.edit { clear() }
    }

    private companion object {
        const val PREFS = "agatha_push"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_UPLOADED = "fcm_token_uploaded"
    }
}
