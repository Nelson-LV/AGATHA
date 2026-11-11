package com.hivend.agatha.data.local.alert

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.Inspection
import com.hivend.agatha.domain.model.MaintenanceRecord
import com.hivend.agatha.domain.model.MaintenanceType
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.PhotoEvidence
import java.time.Instant

/**
 * Reporte de inspección completo guardado en el celular (HU-5.1). De él salen la etiqueta y la
 * observación que la API recibe (ver [AlertActionEntity]); el mantenimiento y las fotos todavía
 * no tienen endpoint y solo viven aquí, para que el detalle y el historial los sigan mostrando
 * después de cerrar la app.
 */
@Entity(tableName = "inspection_reports", indices = [Index("alertId"), Index("deviceId")])
data class InspectionReportEntity(
    @PrimaryKey val id: String,
    val deviceId: String,
    val alertId: String?,
    val tag: AlertTag?,
    val tagOtherDetail: String?,
    val observations: String?,
    /** Tipos de mantenimiento separados por coma (nombres del enum); null si no se hizo. */
    val maintenanceTypes: String?,
    val maintenanceOtherDetail: String?,
    val maintenanceDescription: String?,
    val deviceIdentifier: String,
    val recordedAt: Instant,
)

/** Foto de un reporte, en el orden en que el técnico la agregó. */
@Entity(
    tableName = "inspection_photos",
    primaryKeys = ["reportId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = InspectionReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class InspectionPhotoEntity(
    val reportId: String,
    val position: Int,
    /** Copia comprimida dentro del almacenamiento de la app (ver PhotoProcessor). */
    val uri: String,
    val description: String,
)

internal fun Inspection.toEntity() = InspectionReportEntity(
    id = id,
    deviceId = deviceId,
    alertId = alertId,
    tag = tag,
    tagOtherDetail = tagOtherDetail,
    observations = observations,
    maintenanceTypes = maintenance?.types?.joinToString(",") { it.name },
    maintenanceOtherDetail = maintenance?.otherDetail,
    maintenanceDescription = maintenance?.description,
    deviceIdentifier = (origin as? Origin.MobileApp)?.deviceId.orEmpty(),
    recordedAt = recordedAt,
)

internal fun Inspection.photoEntities() = photos.mapIndexed { position, photo ->
    InspectionPhotoEntity(id, position, photo.uri, photo.description)
}

internal fun InspectionReportEntity.toDomain(photos: List<InspectionPhotoEntity>) = Inspection(
    id = id,
    deviceId = deviceId,
    alertId = alertId,
    tag = tag,
    tagOtherDetail = tagOtherDetail,
    observations = observations,
    maintenance = maintenanceTypes?.let { types ->
        MaintenanceRecord(
            types = types.split(',').map(MaintenanceType::valueOf).toSet(),
            otherDetail = maintenanceOtherDetail,
            description = maintenanceDescription,
        )
    },
    photos = photos.sortedBy { it.position }.map { PhotoEvidence(it.uri, it.description) },
    recordedAt = recordedAt,
    origin = Origin.MobileApp(deviceIdentifier),
)
