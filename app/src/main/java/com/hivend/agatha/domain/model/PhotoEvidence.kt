package com.hivend.agatha.domain.model

/**
 * Evidencia fotográfica de un reporte de inspección (HE-06, opcional). [uri] apunta a un
 * archivo del cache privado de la app (ver res/xml/file_paths.xml) o a la foto elegida en la
 * galería hasta que la sincronización la sube a la API central. [description] es opcional y
 * también se ve en la web (HU-6.2).
 */
data class PhotoEvidence(
    val uri: String,
    val description: String,
) {
    init {
        require(description.length <= DESCRIPTION_MAX_LENGTH) { "Photo description too long" }
    }

    companion object {
        /** HU-6.1: máximo 5 fotografías por reporte. */
        const val MAX_PER_REPORT = 5
        const val DESCRIPTION_MAX_LENGTH = 200
        const val MAX_SIZE_BYTES = 5L * 1024 * 1024
        /** Formatos que acepta la web: JPG/JPEG y PNG. */
        val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png")
    }
}
