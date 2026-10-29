package com.hivend.agatha.data.sync

import com.hivend.agatha.data.local.sync.SyncQueueDao
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.isRetryable
import com.hivend.agatha.domain.model.SyncState
import javax.inject.Inject

/** Resultado de una pasada por la cola: cuántos registros llegaron y si hay que volver a intentar. */
data class SyncRun(val sent: Int, val retryLater: Boolean)

/**
 * Envía la cola de salida en orden de registro (HU-7.2, RN-17), sin depender de WorkManager para
 * poder probarlo en la JVM:
 * - una entrada espera mientras la que indica `dependsOn` no esté Sincronizada (la clasificación
 *   antes que el reporte, el reporte antes que sus fotos);
 * - un fallo reintentable (sin conexión, 408, 429, 5xx) la deja Pendiente y se reintenta
 *   automáticamente hasta [MAX_RETRIES] veces; luego queda en Error con el motivo;
 * - un rechazo del servidor (otro 4xx, respuesta inválida) la deja en Error de una vez. Un 409 es
 *   el conflicto RN-18, que tendrá su propio manejo (HU-7.4);
 * - una entrada en Error solo vuelve a salir con el reintento manual (HU-7.3).
 */
class SyncQueueProcessor @Inject constructor(
    private val queue: SyncQueueDao,
    senders: Set<@JvmSuppressWildcards SyncRecordSender>,
) {
    private val senders = senders.associateBy { it.type }

    suspend fun process(): SyncRun {
        var sent = 0
        var retryLater = false
        val pending = queue.unsynced()
        val stateById = pending.associate { it.id to it.state }.toMutableMap()

        for (entry in pending) {
            if (entry.state == SyncState.ERROR || !dependencyDone(entry, stateById)) continue
            val sender = senders[entry.recordType] ?: continue

            queue.updateState(entry.id, SyncState.SYNCING, entry.attempts, entry.lastError)
            val attempts = entry.attempts + 1
            val newState = when (val result = sender.send(entry.recordId)) {
                is ApiResult.Success -> {
                    sent++
                    queue.updateState(entry.id, SyncState.SYNCED, attempts, lastError = null)
                    SyncState.SYNCED
                }
                is ApiResult.Failure -> {
                    // El primer envío no cuenta como reintento: Error tras 1 + MAX_RETRIES intentos.
                    val retry = result.error.isRetryable && attempts <= MAX_RETRIES
                    val state = if (retry) SyncState.PENDING else SyncState.ERROR
                    if (retry) retryLater = true
                    queue.updateState(entry.id, state, attempts, result.error.reason())
                    state
                }
            }
            stateById[entry.id] = newState
        }
        return SyncRun(sent, retryLater)
    }

    private suspend fun dependencyDone(entry: SyncQueueEntity, stateById: Map<String, SyncState>): Boolean {
        val dependency = entry.dependsOn ?: return true
        // Fuera de la lista de pendientes = ya sincronizada; si no existe, no hay qué esperar.
        val state = stateById[dependency] ?: queue.get(dependency)?.state ?: return true
        return state == SyncState.SYNCED
    }

    /** Motivo técnico guardado con la entrada; la pantalla de sincronización lo traducirá. */
    private fun ApiError.reason(): String = when (this) {
        ApiError.NoConnection -> "NO_CONNECTION"
        ApiError.InvalidResponse -> "INVALID_RESPONSE"
        is ApiError.Http -> listOfNotNull("HTTP $code", body?.let { "${it.code}: ${it.message}" })
            .joinToString(" ")
    }

    companion object {
        /** Reintentos automáticos antes de dejar la entrada en Error (CA-7.2.4). */
        const val MAX_RETRIES = 3
    }
}
