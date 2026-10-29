package com.hivend.agatha.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Programa [SyncWorker] con WorkManager, siempre con la restricción de red: sin conexión el
 * trabajo espera y sale solo al recuperarla (CA-7.2.1), aunque la app esté cerrada.
 */
@Singleton
class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Al abrir la app: un envío en cuanto haya red y otro cada 15 minutos como respaldo. */
    fun schedule() {
        syncNow()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(needsNetwork)
                .build(),
        )
    }

    /**
     * Pide un envío en cuanto haya red. Se llama después de encolar un registro; si ya hay uno
     * programado o en curso no se duplica (KEEP), porque el worker recoge lo nuevo al terminar.
     */
    fun syncNow() = enqueueOneTime(ExistingWorkPolicy.KEEP)

    /**
     * Reintento pedido por el técnico (HU-7.3): reemplaza un envío que esté esperando su próximo
     * reintento automático, para no hacerlo esperar. Si había un envío en curso se cancela y se
     * repite; es seguro porque el servidor no duplica un registro con el mismo id.
     */
    fun retryNow() = enqueueOneTime(ExistingWorkPolicy.REPLACE)

    private fun enqueueOneTime(policy: ExistingWorkPolicy) {
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK,
            policy,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(needsNetwork)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    private companion object {
        const val ONE_TIME_WORK = "agatha-sync-now"
        const val PERIODIC_WORK = "agatha-sync-periodic"
    }
}
