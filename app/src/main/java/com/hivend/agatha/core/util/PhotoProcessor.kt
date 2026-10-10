package com.hivend.agatha.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.hivend.agatha.domain.model.PhotoEvidence
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resultado de preparar una foto para un reporte (HU-6.1). */
sealed interface PreparedPhoto {
    /** Copia local lista para el reporte, ≤ 5 MB. */
    data class Accepted(val uri: String) : PreparedPhoto
    data class Rejected(val fileName: String, val reason: Reason) : PreparedPhoto

    enum class Reason { FORMAT_NOT_ALLOWED, UNREADABLE }
}

/**
 * Prepara cada foto elegida o tomada para un reporte de inspección (HE-06): valida el formato
 * de la web (JPG/JPEG/PNG), la copia al cache privado — así sigue disponible sin conexión hasta
 * que la sincronización la suba — y la comprime a JPEG si pasa de 5 MB.
 */
@Singleton
class PhotoProcessor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun prepare(uri: Uri): PreparedPhoto = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = displayName(uri)
        val mime = resolver.getType(uri)
        if (mime !in PhotoEvidence.ALLOWED_MIME_TYPES) {
            return@withContext PreparedPhoto.Rejected(name, PreparedPhoto.Reason.FORMAT_NOT_ALLOWED)
        }
        runCatching {
            val folder = File(context.cacheDir, "evidencias").apply { mkdirs() }
            val extension = if (mime == "image/png") "png" else "jpg"
            val copy = File(folder, "AGATHA_${UUID.randomUUID()}.$extension")
            resolver.openInputStream(uri)!!.use { input -> copy.outputStream().use { input.copyTo(it) } }
            val result = if (copy.length() > PhotoEvidence.MAX_SIZE_BYTES) compress(copy) else copy
            PreparedPhoto.Accepted(result.toUri().toString())
        }.getOrElse { PreparedPhoto.Rejected(name, PreparedPhoto.Reason.UNREADABLE) }
    }

    /** Reduce resolución y calidad hasta quedar por debajo del límite de la web. */
    private fun compress(source: File): File {
        val target = File(source.parentFile, source.nameWithoutExtension + "_c.jpg")
        var sampleSize = 1
        var quality = 85
        while (true) {
            val bitmap = BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
                ?: error("Cannot decode ${source.name}")
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            bitmap.recycle()
            if (target.length() <= PhotoEvidence.MAX_SIZE_BYTES) break
            if (quality > 60) quality -= 10 else sampleSize *= 2
        }
        source.delete()
        return target
    }

    private fun displayName(uri: Uri): String =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()
}
