package com.hivend.agatha.domain.model

import java.time.Instant

/** Qué tipo de registro espera en la cola de sincronización. Se persiste por nombre: no renombrar. */
enum class SyncRecordType {
    CLASSIFICATION,
    INSPECTION_REPORT,
    OBSERVATION,
    PHOTO,
    PUSH_TOKEN,
}

/** Por qué falló el último envío de un registro (HU-7.2), para mostrar el motivo del Error. */
sealed interface SyncError {
    data object NoConnection : SyncError
    data object InvalidResponse : SyncError

    /** El servidor respondió [httpCode]: rechazo (4xx) o falla del servidor (5xx). */
    data class Server(val httpCode: Int) : SyncError
}

/** Un registro creado en el celular que todavía no está sincronizado con la API (HE-07). */
data class PendingRecord(
    val id: String,
    val type: SyncRecordType,
    val state: SyncState,
    val createdAt: Instant,
    val attempts: Int,
    /** Motivo del último fallo; null si todavía no ha fallado. */
    val error: SyncError?,
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
    /** Registros sin sincronizar, en orden de registro. */
    val pending: List<PendingRecord>,
    val conflict: SyncConflict?,
    /** Última vez que el envío terminó sin nada por reintentar; null si nunca. */
    val lastSuccessfulSync: Instant?,
) {
    val hasErrors: Boolean get() = pending.any { it.state == SyncState.ERROR }
}
