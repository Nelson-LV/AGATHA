package com.hivend.agatha.data.local.alert

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.SyncRecordType
import java.time.Instant

/**
 * Un envío a la API de gestión de alertas hecho en el celular y guardado para la cola de salida
 * (HU-7.2): la clasificación del detalle, o la etiqueta y la observación de un reporte de
 * inspección. Se encola en `sync_queue` con el mismo [id] como `recordId` y [type] como
 * `recordType`; lo envía su [com.hivend.agatha.data.sync.SyncRecordSender].
 *
 * Según [type] se llenan unos campos u otros:
 * - [SyncRecordType.CLASSIFICATION]: [classification].
 * - [SyncRecordType.INSPECTION_REPORT]: la etiqueta del reporte, [tag] y [tagOtherDetail].
 * - [SyncRecordType.OBSERVATION]: [text].
 */
@Entity(tableName = "alert_actions", indices = [Index("alertId")])
data class AlertActionEntity(
    /** UUID del registro; la clasificación y la etiqueta lo mandan como `client_record_id`. */
    @PrimaryKey val id: String,
    val alertId: String,
    val type: SyncRecordType,
    /** Reporte de inspección del que sale la etiqueta o la observación; null si no viene de uno. */
    val inspectionId: String? = null,
    val classification: AlertClassification? = null,
    val tag: AlertTag? = null,
    val tagOtherDetail: String? = null,
    val text: String? = null,
    val deviceIdentifier: String,
    val recordedAt: Instant,
)
