package com.hivend.agatha.data.export

import android.content.Context
import com.hivend.agatha.domain.model.AlertExportFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dónde queda en el celular un Excel exportado antes de compartirlo. */
interface ExportFileStore {
    suspend fun save(file: AlertExportFile): File
}

/**
 * Guarda el Excel en la caché privada de la app (carpeta "exportaciones", la que comparte el
 * FileProvider) y borra los anteriores: solo sirve para pasárselo a la app que elija el técnico.
 */
@Singleton
class CacheExportFileStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ExportFileStore {

    override suspend fun save(file: AlertExportFile): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        File(directory, safeName(file.fileName)).apply { writeBytes(file.bytes) }
    }

    private fun safeName(name: String): String {
        val cleaned = name.replace(Regex("""[^\w.\-]"""), "_").trim('.', '_')
        return if (cleaned.endsWith(EXTENSION, ignoreCase = true)) cleaned else cleaned.ifEmpty { "alertas" } + EXTENSION
    }

    private companion object {
        const val DIRECTORY = "exportaciones"
        const val EXTENSION = ".xlsx"
    }
}
