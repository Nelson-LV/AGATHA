package com.hivend.agatha.data.repository

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
                PendingRecord("p1", "Inspección · Punto 3", "Registrada 12:58 · esperando conexión"),
                PendingRecord("p2", "Foto evidencia · Punto 2", "2 imágenes · registradas 13:04"),
                PendingRecord("p3", "Clasificación · Punto 1", "Fuga confirmada · error al enviar", error = true),
            ),
            conflict = SyncConflict(
                alertId = "MP-1122",
                conflictTitle = "Conflicto detectado · Punto 1",
                description = "Esta alerta fue modificada también desde la plataforma web mientras estabas sin conexión.",
                localChange = "Clasificación: Fuga confirmada · 13:10",
                serverChange = "Estado: Cerrada por Operador Web · 13:12",
            ),
            lastSuccessfulSync = "hace 2 h",
        )
    )

    override fun observeStatus() = status

    override suspend fun syncNow() {
        delay(600) // Simula la latencia de red mientras no hay WorkManager real.
        status.update {
            it.copy(connected = true, pending = emptyList(), lastSuccessfulSync = "justo ahora")
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
