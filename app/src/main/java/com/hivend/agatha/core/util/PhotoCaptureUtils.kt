package com.hivend.agatha.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Crea el archivo destino para una foto tomada con la cámara del sistema (HE-06). Vive en
 * el cache privado de la app — ver res/xml/file_paths.xml — y se comparte con la app de
 * cámara únicamente a través de un content:// URI de [FileProvider], nunca de una ruta
 * file:// directa (requisito de Android 7+ y buena práctica de seguridad).
 */
fun createEvidenceUri(context: Context): Uri {
    val folder = File(context.cacheDir, "evidencias").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(folder, "AGATHA_$timestamp.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
