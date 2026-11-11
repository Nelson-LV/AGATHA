package com.hivend.agatha.push

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Último token FCM del celular. El backend lo necesita junto al id del dispositivo
 * ([com.hivend.agatha.core.device.DeviceOriginProvider]) para mandarle los push; lo registra
 * [PushTokenRegistrar].
 */
@Singleton
class PushTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)

    fun save(token: String) = prefs.edit { putString(KEY_TOKEN, token) }

    /** FCM dio de baja [token]: se olvida si era el guardado. */
    fun clear(token: String) {
        if (token == this.token) prefs.edit { clear() }
    }

    private companion object {
        const val PREFS = "agatha_push"
        const val KEY_TOKEN = "fcm_token"
    }
}
