package com.hivend.agatha.data.local.sync

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.domain.model.SyncState
import java.time.Instant

/**
 * Entrada de la cola de salida (HU-7.2, RN-17). Cada feature guarda su registro en su propia
 * tabla y encola aquí una referencia a él; el motor de sincronización (data/sync) envía las
 * entradas en orden de [createdAt] y respeta [dependsOn] (la clasificación antes que el reporte,
 * el reporte antes que sus fotos). Un mismo registro se encola una sola vez.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["recordType", "recordId"], unique = true),
        Index("state"),
        Index("dependsOn"),
    ],
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val recordType: SyncRecordType,
    /** UUID generado en el celular para el registro; la API lo usa para no duplicarlo en un reintento. */
    val recordId: String,
    /** [id] de la entrada que debe sincronizarse antes que esta. */
    val dependsOn: String? = null,
    val createdAt: Instant,
    val state: SyncState = SyncState.PENDING,
    val attempts: Int = 0,
    /** Motivo del último fallo (red o respuesta de la API); se muestra cuando la entrada queda en Error. */
    val lastError: String? = null,
)
