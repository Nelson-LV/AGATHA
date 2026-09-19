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
fun crearUriParaEvidencia(context: Context): Uri {
    val carpeta = File(context.cacheDir, "evidencias").apply { mkdirs() }
    val marcaDeTiempo = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val archivo = File(carpeta, "AGATHA_$marcaDeTiempo.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
