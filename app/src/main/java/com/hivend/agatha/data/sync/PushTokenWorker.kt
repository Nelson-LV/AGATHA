package com.hivend.agatha.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hivend.agatha.data.remote.ApiError
import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.data.remote.isRetryable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registra el token FCM en el backend (docs/CONTRATO_PUSH_FCM.md § 1). No pasa por la cola de
 * sincronización: un 4xx es definitivo y se recupera solo en el siguiente arranque, así que no es
 * algo que el técnico deba ver o reintentar (hoy el servidor del piloto responde 404).
 */
@HiltWorker
class PushTokenWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sender: PushTokenSender,
    private val scheduler: PushTokenScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val failures = inputData.getInt(KEY_FAILURES, 0)
        val result = sender.send()
        if (result is ApiResult.Failure) {
            val delay = pushTokenRetryDelay(result.error, failures)
            if (delay != null) {
                scheduler.retry(failures + 1, delay)
            } else {
                Log.w(TAG, "Push token registration discarded: ${result.error}")
            }
        }
        return Result.success()
    }

    companion object {
        internal const val KEY_FAILURES = "failures"
        private const val TAG = "AgathaPush"
    }
}

/**
 * Programa [PushTokenWorker], siempre con la restricción de red: sin conexión espera y sale solo
 * al recuperarla, aunque la app esté cerrada.
 */
@Singleton
class PushTokenScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val needsNetwork = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Registro nuevo (arranque o token renovado): reemplaza un reintento que estuviera esperando. */
    fun register() = enqueue(ExistingWorkPolicy.REPLACE, failures = 0, delay = Duration.ZERO)

    /**
     * Reintento después de [delay]. Se encadena al worker en curso (WorkManager tiene un solo
     * backoff, con tope de 5 h y sin `Retry-After`), y empieza cuando ese termina.
     */
    fun retry(failures: Int, delay: Duration) = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE, failures, delay)

    private fun enqueue(policy: ExistingWorkPolicy, failures: Int, delay: Duration) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            policy,
            OneTimeWorkRequestBuilder<PushTokenWorker>()
                .setConstraints(needsNetwork)
                .setInitialDelay(delay)
                .setInputData(workDataOf(PushTokenWorker.KEY_FAILURES to failures))
                .build(),
        )
    }

    private companion object {
        const val UNIQUE_WORK = "agatha-push-token"
    }
}

/**
 * Cuánto esperar antes de reintentar tras [failures] fallos previos, o null si no se reintenta
 * (cualquier otro 4xx o una respuesta inválida: el mismo cuerpo volvería a fallar). La espera se
 * duplica desde 30 s hasta 1 h y nunca es menor que el `Retry-After` del servidor.
 */
internal fun pushTokenRetryDelay(error: ApiError, failures: Int): Duration? {
    if (!error.isRetryable) return null
    val backoff = FIRST_RETRY.multipliedBy(1L shl failures.coerceIn(0, MAX_DOUBLINGS)).coerceAtMost(MAX_RETRY)
    val retryAfter = (error as? ApiError.Http)?.retryAfter ?: return backoff
    return maxOf(backoff, retryAfter)
}

private val FIRST_RETRY = Duration.ofSeconds(30)
private val MAX_RETRY = Duration.ofHours(1)

/** 30 s × 2^7 ya supera 1 h: evita desbordar el desplazamiento con muchos fallos. */
private const val MAX_DOUBLINGS = 7
