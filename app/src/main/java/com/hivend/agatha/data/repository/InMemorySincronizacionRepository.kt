package com.hivend.agatha.data.repository

import com.hivend.agatha.domain.model.ConflictoSincronizacion
import com.hivend.agatha.domain.model.EstadoSincronizacion
import com.hivend.agatha.domain.model.RegistroPendiente
import com.hivend.agatha.domain.repository.SincronizacionRepository
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
class InMemorySincronizacionRepository @Inject constructor() : SincronizacionRepository {

    private val estado = MutableStateFlow(
        EstadoSincronizacion(
            conectado = false,
            pendientes = listOf(
                RegistroPendiente("p1", "Inspección · Punto 3", "Registrada 12:58 · esperando conexión"),
                RegistroPendiente("p2", "Foto evidencia · Punto 2", "2 imágenes · registradas 13:04"),
                RegistroPendiente("p3", "Clasificación · Punto 1", "Fuga confirmada · error al enviar", error = true),
            ),
            conflicto = ConflictoSincronizacion(
                alertaId = "MP-1122",
                tituloConflicto = "Conflicto detectado · Punto 1",
                descripcion = "Esta alerta fue modificada también desde la plataforma web mientras estabas sin conexión.",
                cambioLocal = "Clasificación: Fuga confirmada · 13:10",
                cambioServidor = "Estado: Cerrada por Operador Web · 13:12",
            ),
            ultimaSincronizacionExitosa = "hace 2 h",
        )
    )

    override fun observarEstado() = estado

    override suspend fun sincronizarAhora() {
        delay(600) // Simula la latencia de red mientras no hay WorkManager real.
        estado.update {
            it.copy(conectado = true, pendientes = emptyList(), ultimaSincronizacionExitosa = "justo ahora")
        }
    }

    override suspend fun reintentar(registroId: String) {
        delay(300)
        estado.update { actual ->
            actual.copy(pendientes = actual.pendientes.filterNot { it.id == registroId })
        }
    }

    override suspend fun confirmarConflicto(alertaId: String) {
        estado.update { it.copy(conflicto = null) }
    }
}
