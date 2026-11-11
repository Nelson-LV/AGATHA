package com.hivend.agatha.domain.model

/**
 * Quién creó un registro (RN-10, Backlog 1.3). No hay inicio de sesión: en lugar del usuario
 * cada registro guarda su fecha-hora y su origen. La web registra [Web]; la app registra
 * [MobileApp] con el identificador del celular configurado en la app.
 */
sealed interface Origin {
    data object Web : Origin
    data class MobileApp(val deviceId: String) : Origin
}
