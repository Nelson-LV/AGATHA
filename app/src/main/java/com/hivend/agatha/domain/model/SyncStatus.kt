package com.hivend.agatha.domain.model

/** Un registro creado sin conexión que todavía no llega a la API central (HE-07). */
data class PendingRecord(
    val id: String,
    val title: LocalizedText,
    val detail: LocalizedText,
    val error: Boolean = false,
)

/** Conflicto detectado cuando la misma alerta cambió en la app y en la plataforma web. */
data class SyncConflict(
    val alertId: String,
    val conflictTitle: LocalizedText,
    val description: LocalizedText,
    val localChange: LocalizedText,
    val serverChange: LocalizedText,
)

/** Estado agregado de la bandeja de sincronización, consumido por [com.hivend.agatha.ui.sync.SyncScreen]. */
data class SyncStatus(
    val connected: Boolean,
    val pending: List<PendingRecord>,
    val conflict: SyncConflict?,
    /** Minutos desde la última sincronización correcta (0 = justo ahora). */
    val lastSuccessfulSyncMinutesAgo: Int,
)
