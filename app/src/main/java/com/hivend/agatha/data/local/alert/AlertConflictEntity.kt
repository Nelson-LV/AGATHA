package com.hivend.agatha.data.local.alert

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.hivend.agatha.domain.model.ActionConflict
import com.hivend.agatha.domain.model.AlertClassification
import com.hivend.agatha.domain.model.AlertTag
import com.hivend.agatha.domain.model.ClassificationRecord
import com.hivend.agatha.domain.model.Origin
import com.hivend.agatha.domain.model.SyncRecordType
import com.hivend.agatha.domain.model.TagRecord
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Conflicto de un envío de la cola (RN-18) guardado para avisarlo aunque la app se haya cerrado
 * mientras lo enviaba WorkManager. Queda hasta que el técnico lo marca como visto.
 */
@Entity(tableName = "alert_conflicts", indices = [Index("alertId")])
data class AlertConflictEntity(
    @PrimaryKey val id: String,
    val alertId: String,
    val type: SyncRecordType,
    val localClassification: AlertClassification?,
    val localTag: AlertTag?,
    val localTagDetail: String?,
    val existingClassification: AlertClassification?,
    val existingClassifiedAt: Instant?,
    /** "WEB" o el id del celular que la registró; null si no se sabe. */
    val existingOrigin: String?,
    val existingTag: AlertTag?,
    val existingTagDetail: String?,
    val detectedAt: Instant,
    val acknowledged: Boolean = false,
)

@Dao
interface AlertConflictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: AlertConflictEntity)

    /** Conflictos que el técnico aún no vio, el más reciente primero. */
    @Query("SELECT * FROM alert_conflicts WHERE acknowledged = 0 ORDER BY detectedAt DESC")
    fun observeOpen(): Flow<List<AlertConflictEntity>>

    @Query("UPDATE alert_conflicts SET acknowledged = 1 WHERE alertId = :alertId")
    suspend fun acknowledge(alertId: String)
}

private const val ORIGIN_WEB = "WEB"

internal fun ActionConflict.toEntity(): AlertConflictEntity {
    val tag = existing?.tag
    return AlertConflictEntity(
        id = id,
        alertId = alertId,
        type = type,
        localClassification = localClassification,
        localTag = localTag,
        localTagDetail = localTagDetail,
        existingClassification = existing?.classification,
        existingClassifiedAt = existing?.classifiedAt,
        existingOrigin = when (val origin = existing?.origin) {
            null -> null
            Origin.Web -> ORIGIN_WEB
            is Origin.MobileApp -> origin.deviceId
        },
        existingTag = tag?.tag,
        existingTagDetail = tag?.otherDetail,
        detectedAt = detectedAt,
    )
}

internal fun AlertConflictEntity.toDomain(): ActionConflict {
    val origin = when (existingOrigin) {
        null -> null
        ORIGIN_WEB -> Origin.Web
        else -> Origin.MobileApp(existingOrigin)
    }
    val existing = existingClassification?.let { classification ->
        val tag = existingTag?.takeIf { it.classification == classification }?.let {
            runCatching { TagRecord(it, existingTagDetail.takeIf { _ -> it.requiresDetail }, null, origin) }.getOrNull()
        }
        ClassificationRecord(classification, existingClassifiedAt, origin, tag)
    }
    return ActionConflict(
        id = id,
        alertId = alertId,
        type = type,
        localClassification = localClassification,
        localTag = localTag,
        localTagDetail = localTagDetail,
        existing = existing,
        detectedAt = detectedAt,
    )
}
