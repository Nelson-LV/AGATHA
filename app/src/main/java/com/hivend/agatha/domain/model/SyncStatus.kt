package com.hivend.agatha.domain.model

/** Un registro creado sin conexión que todavía no llega a la API central (HE-07). */
data class PendingRecord(
    val id: String,
    val title: String,
    val detail: String,
    val error: Boolean = false,
)

/** Conflicto detectado cuando la misma alerta cambió en la app y en la plataforma web. */
data class SyncConflict(
    val alertId: String,
    val conflictTitle: String,
    val description: String,
    val localChange: String,
    val serverChange: String,
)

/** Estado agregado de la bandeja de sincronización, consumido por [com.hivend.agatha.ui.sync.SyncScreen]. */
data class SyncStatus(
    val connected: Boolean,
    val pending: List<PendingRecord>,
    val conflict: SyncConflict?,
    val lastSuccessfulSync: String,
)
