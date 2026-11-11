package com.hivend.agatha.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant

/**
 * Envío en segundo plano de la cola de salida (HU-7.2). Solo corre con red (ver
 * [SyncScheduler]) y sobrevive al cierre de la app. Repite la pasada mientras lleguen registros,
 * para recoger lo que se encoló durante el envío; si algo falló de forma reintentable, pide a
 * WorkManager otro intento con espera exponencial. Una pasada sin nada por reintentar queda como
 * la última sincronización correcta.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: SyncQueueProcessor,
    private val lastSync: LastSyncStore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var run: SyncRun
        do {
            run = processor.process()
        } while (run.sent > 0 && !run.retryLater)
        if (run.retryLater) return Result.retry()
        lastSync.markSynced(Instant.now())
        return Result.success()
    }
}
