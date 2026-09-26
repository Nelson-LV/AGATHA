package com.hivend.agatha.domain.model

/**
 * Evidencia fotográfica adjunta a una inspección (HE-06, opcional). [uri] apunta a un
 * archivo del cache privado de la app (ver res/xml/file_paths.xml) hasta que la
 * sincronización la sube a la API central y pasa a referenciar la URL remota.
 */
data class PhotoEvidence(
    val uri: String,
    val description: String,
)
