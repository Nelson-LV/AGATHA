package com.hivend.agatha.domain.model

/** Por qué una operación contra la API de alertas no se completó. */
sealed interface AlertFailure {
    /** Sin red o tiempo de espera agotado: el envío queda pendiente y se reintenta. */
    data object NoConnection : AlertFailure

    /** El servidor respondió con un error 5xx o con algo que la app no entiende. */
    data object Server : AlertFailure

    /** La alerta no existe (404). */
    data object NotFound : AlertFailure

    /** El servidor rechazó el envío (4xx). [message] viene de la API, en español, listo para mostrar. */
    data class Rejected(val code: String?, val message: String?) : AlertFailure
}

sealed interface AlertResult<out T> {
    data class Success<T>(val value: T) : AlertResult<T>
    data class Failure(val failure: AlertFailure) : AlertResult<Nothing>
}

/** Qué pasó al enviar una clasificación (HU-5.2). */
sealed interface ClassifyOutcome {
    /** Quedó guardada en el servidor, o ya la tenía (reintento del mismo registro). */
    data object Saved : ClassifyOutcome

    /**
     * La alerta ya estaba clasificada desde otro cliente: la clasificación de este celular se
     * guardó como observación de conflicto (RN-06, RN-18). [existing] es la que prevalece.
     */
    data class SavedAsConflict(val existing: ClassificationRecord) : ClassifyOutcome

    /** Sin conexión: se mostró localmente y queda pendiente de enviar con el mismo registro. */
    data object QueuedOffline : ClassifyOutcome
}
