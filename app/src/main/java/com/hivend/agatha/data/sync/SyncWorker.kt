package com.hivend.agatha.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Envío en segundo plano de la cola de salida (HU-7.2). Solo corre con red (ver
 * [SyncScheduler]) y sobrevive al cierre de la app. Repite la pasada mientras lleguen registros,
 * para recoger lo que se encoló durante el envío; si algo falló de forma reintentable, pide a
 * WorkManager otro intento con espera exponencial.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val processor: SyncQueueProcessor,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        var run: SyncRun
        do {
            run = processor.process()
        } while (run.sent > 0 && !run.retryLater)
        return if (run.retryLater) Result.retry() else Result.success()
    }
}
