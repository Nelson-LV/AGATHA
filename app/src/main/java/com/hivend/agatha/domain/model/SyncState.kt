package com.hivend.agatha.domain.model

/**
 * Estado de sincronización de un registro creado en el celular (RN-17): nace [PENDING], pasa a
 * [SYNCING] mientras se envía y termina en [SYNCED] o, tras agotar los reintentos, en [ERROR].
 */
enum class SyncState {
    PENDING,
    SYNCING,
    SYNCED,
    ERROR,
}
