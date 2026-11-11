package com.hivend.agatha.data.local.alert

import androidx.room.withTransaction
import com.hivend.agatha.data.local.AgathaDatabase
import com.hivend.agatha.data.local.sync.SyncQueueEntity
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.data.sync.SyncScheduler
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Lo que el celular guarda de las alertas (HU-7.2, RN-17): los envíos hasta que llegan a la API
 * y los reportes de inspección completos, con el mantenimiento y las fotos que la API todavía no
 * recibe. Separa a [com.hivend.agatha.data.repository.RemoteAlertRepository] de Room y
 * WorkManager para probarlo en la JVM.
 */
interface AlertOutbox {
    /** Reportes de inspección guardados en el celular, el más reciente primero. */
    fun observeReports(): Flow<List<Inspection>>

    suspend fun saveReport(report: Inspection)

    fun observeUnsynced(): Flow<List<AlertActionEntity>>

    suspend fun get(id: String): AlertActionEntity?

    /**
     * Guarda [actions] y las encola en ese orden. Una etiqueta espera a la clasificación de su
     * alerta si esta sigue en la cola. Pide un envío en cuanto haya red.
     */
    suspend fun enqueue(actions: List<AlertActionEntity>)
}

@Singleton
class RoomAlertOutbox @Inject constructor(
    private val database: AgathaDatabase,
    private val scheduler: SyncScheduler,
) : AlertOutbox {

    private val actions get() = database.alertActionDao()
    private val queue get() = database.syncQueueDao()
    private val reports get() = database.inspectionReportDao()

    override fun observeReports(): Flow<List<Inspection>> =
        combine(reports.observeReports(), reports.observePhotos()) { rows, photos ->
            val photosByReport = photos.groupBy { it.reportId }
            rows.map { it.toDomain(photosByReport[it.id].orEmpty()) }
        }

    override suspend fun saveReport(report: Inspection) = database.withTransaction {
        reports.insertReport(report.toEntity())
        reports.insertPhotos(report.photoEntities())
    }

    override fun observeUnsynced(): Flow<List<AlertActionEntity>> = actions.observeUnsynced()

    override suspend fun get(id: String): AlertActionEntity? = actions.get(id)

    override suspend fun enqueue(actions: List<AlertActionEntity>) {
        if (actions.isEmpty()) return
        database.withTransaction {
            val now = Instant.now()
            actions.forEachIndexed { index, action ->
                this.actions.insert(action)
                val dependsOn = if (action.type == SyncRecordType.INSPECTION_REPORT) {
                    this.actions.unsyncedClassificationEntry(action.alertId)
                } else {
                    null
                }
                queue.enqueue(
                    SyncQueueEntity(
                        id = UUID.randomUUID().toString(),
                        recordType = action.type,
                        recordId = action.id,
                        dependsOn = dependsOn,
                        // Mismo instante para todo el lote: el índice conserva el orden (etiqueta, observación).
                        createdAt = now.plusMillis(index.toLong()),
                    ),
                )
            }
        }
        scheduler.syncNow()
    }
}
