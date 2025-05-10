package com.uv.sanuvia.ui.screens.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.uv.sanuvia.BuildConfig // Asegúrate que esta ruta es correcta
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Función auxiliar para crear Uri temporal ---
fun Context.createImageUri(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    // Guarda en un subdirectorio "images" dentro del directorio de caché de la app
    val storageDir = File(cacheDir, "images")
    if (!storageDir.exists()) {
        storageDir.mkdirs() // Crea el directorio si no existe
    }
    val imageFile = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg",        /* suffix */
        storageDir     /* directory */
    )
    // La authority DEBE coincidir con la definida en tu AndroidManifest.xml
    return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", imageFile)
}
