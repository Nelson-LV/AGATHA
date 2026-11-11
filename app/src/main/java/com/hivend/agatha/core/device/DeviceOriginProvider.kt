package com.hivend.agatha.core.device

import android.content.Context
import androidx.core.content.edit
import com.hivend.agatha.domain.model.Origin
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Origen con el que la app firma sus registros (RN-10): "App móvil" + el identificador del
 * celular. No hay login, así que el id se genera una vez y queda guardado en la app; una
 * pantalla de ajustes podrá permitir cambiarlo por el que asigne el operador.
 */
@Singleton
class DeviceOriginProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: newDeviceId().also { id ->
            prefs.edit { putString(KEY_DEVICE_ID, id) }
        }

    fun current(): Origin = Origin.MobileApp(deviceId)

    private fun newDeviceId() = "CEL-" + UUID.randomUUID().toString().take(6).uppercase()

    private companion object {
        const val PREFS = "agatha_device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
