package com.hivend.agatha.data.sync

import com.hivend.agatha.data.local.alert.AlertOutbox
import com.hivend.agatha.data.local.alert.RoomAlertOutbox
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.repository.RemoteAlertRepository
import com.hivend.agatha.domain.model.SyncRecordType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Emisores de los envíos de alertas guardados en `alert_actions`: clasificación, etiqueta del
 * reporte de inspección y observación. Los tres delegan en RemoteAlertRepository, que arma el
 * cuerpo de cada endpoint y refresca la alerta cuando el servidor lo recibe.
 */

class ClassificationSender @Inject constructor(private val alerts: RemoteAlertRepository) : SyncRecordSender {
    override val type = SyncRecordType.CLASSIFICATION
    override suspend fun send(recordId: String): ApiResult<Unit> = alerts.sendQueued(recordId)
}

/** La etiqueta es lo que el reporte de inspección envía a la API (el mantenimiento aún no tiene endpoint). */
class InspectionLabelSender @Inject constructor(private val alerts: RemoteAlertRepository) : SyncRecordSender {
    override val type = SyncRecordType.INSPECTION_REPORT
    override suspend fun send(recordId: String): ApiResult<Unit> = alerts.sendQueued(recordId)
}

class ObservationSender @Inject constructor(private val alerts: RemoteAlertRepository) : SyncRecordSender {
    override val type = SyncRecordType.OBSERVATION
    override suspend fun send(recordId: String): ApiResult<Unit> = alerts.sendQueued(recordId)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AlertSyncModule {

    @Binds
    @Singleton
    abstract fun bindAlertOutbox(impl: RoomAlertOutbox): AlertOutbox

    @Binds
    @IntoSet
    abstract fun bindClassificationSender(impl: ClassificationSender): SyncRecordSender

    @Binds
    @IntoSet
    abstract fun bindInspectionLabelSender(impl: InspectionLabelSender): SyncRecordSender

    @Binds
    @IntoSet
    abstract fun bindObservationSender(impl: ObservationSender): SyncRecordSender
}
