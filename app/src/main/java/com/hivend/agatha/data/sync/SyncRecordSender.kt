package com.hivend.agatha.data.sync

import com.hivend.agatha.data.remote.ApiResult
import com.hivend.agatha.domain.model.SyncRecordType
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Envía a la API un tipo de registro de la cola. Cada feature implementa el suyo (clasificación,
 * reporte, foto, token push…) y lo registra con `@Binds @IntoSet` en un módulo de Hilt; un tipo
 * sin emisor se queda Pendiente hasta que exista su endpoint.
 */
interface SyncRecordSender {
    val type: SyncRecordType

    /** Envía el registro [recordId] de su tabla, con su UUID para que el servidor no lo duplique. */
    suspend fun send(recordId: String): ApiResult<Unit>
}

/** Declara el conjunto de emisores para que exista aunque todavía ninguna feature aporte uno. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncSenderModule {

    @Multibinds
    abstract fun syncRecordSenders(): Set<SyncRecordSender>
}
