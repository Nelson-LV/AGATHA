package com.hivend.agatha.domain.model

import java.time.Instant

/**
 * Una clasificación o etiqueta de la cola que llegó cuando otro cliente ya la había registrado
 * (RN-18, guía §7.5: 409 SAVED_AS_CONFLICT_OBSERVATION). Prevalece la primera; lo de este
 * celular quedó guardado en el servidor como observación. Se avisa al técnico hasta que lo vea.
 */
data class ActionConflict(
    /** Id del envío de la cola (su `client_record_id`). */
    val id: String,
    val alertId: String,
    /** [SyncRecordType.CLASSIFICATION] o [SyncRecordType.INSPECTION_REPORT] (la etiqueta). */
    val type: SyncRecordType,
    val localClassification: AlertClassification? = null,
    val localTag: AlertTag? = null,
    val localTagDetail: String? = null,
    /** Lo que prevalece en el servidor (`existing`); null si la respuesta no lo trajo. */
    val existing: ClassificationRecord?,
    val detectedAt: Instant,
)
