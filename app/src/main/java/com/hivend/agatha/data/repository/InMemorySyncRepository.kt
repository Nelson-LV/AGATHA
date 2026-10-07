package com.hivend.agatha.data.repository

import androidx.annotation.StringRes
import com.hivend.agatha.R
import com.hivend.agatha.domain.model.LocalizedText
import com.hivend.agatha.domain.model.SyncConflict
import com.hivend.agatha.domain.model.SyncStatus
import com.hivend.agatha.domain.model.PendingRecord
import com.hivend.agatha.domain.repository.SyncRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Simula la cola de sincronización de HE-07: arranca "sin conexión" con 3 pendientes y un
 * conflicto, tal como en el prototipo, para que la pantalla de Sync sea interactiva
 * (reintentar / sincronizar ahora / confirmar conflicto) sin depender aún de WorkManager
 * ni de la API real.
 */
@Singleton
class InMemorySyncRepository @Inject constructor() : SyncRepository {

    private val status = MutableStateFlow(
        SyncStatus(
            connected = false,
            pending = listOf(
                PendingRecord(
                    "p1",
                    text(R.string.sample_sync_inspection_title, 3),
                    text(R.string.sample_sync_inspection_detail, "12:58"),
                ),
                PendingRecord(
                    "p2",
                    text(R.string.sample_sync_evidence_title, 2),
                    text(R.string.sample_sync_evidence_detail, 2, "13:04"),
                ),
                PendingRecord(
                    "p3",
                    text(R.string.sample_sync_classification_title, 1),
                    text(R.string.sample_sync_classification_detail),
                    error = true,
                ),
            ),
            conflict = SyncConflict(
                alertId = "MP-1122",
                conflictTitle = text(R.string.sample_sync_conflict_title, 1),
                description = text(R.string.sample_sync_conflict_description),
                localChange = text(R.string.sample_sync_conflict_local, "13:10"),
                serverChange = text(R.string.sample_sync_conflict_server, "13:12"),
            ),
            lastSuccessfulSyncMinutesAgo = 120,
        )
    )

    private fun text(@StringRes id: Int, vararg args: Any) = LocalizedText.Resource(id, args.toList())

    override fun observeStatus() = status

    override suspend fun syncNow() {
        delay(600) // Simula la latencia de red mientras no hay WorkManager real.
        status.update {
            it.copy(connected = true, pending = emptyList(), lastSuccessfulSyncMinutesAgo = 0)
        }
    }

    override suspend fun retry(recordId: String) {
        delay(300)
        status.update { current ->
            current.copy(pending = current.pending.filterNot { it.id == recordId })
        }
    }

    override suspend fun confirmConflict(alertId: String) {
        status.update { it.copy(conflict = null) }
    }
}
