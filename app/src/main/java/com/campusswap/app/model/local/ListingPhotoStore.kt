package com.campusswap.app.model.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.tracing.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Fotos de una guardadas */
class ListingPhotoStore(private val context: Context) {

    private val folder: File get() = File(context.filesDir, "listing-photos").apply { mkdirs() }

    /** Archivo donde la camara guarda la foto: el Uri = camara, guarda ruta */
    fun newCameraTarget(): Pair<Uri, String> {
        val file = newFile()
        return FileProvider.getUriForFile(context, "${context.packageName}.photos", file) to file.absolutePath
    }

    suspend fun finishCameraPhoto(path: String): String? = withContext(Dispatchers.IO) {
        File(path).takeIf { it.length() > 0 }?.let { trace("ListingPhotoStore.shrink") { shrink(it) } }    }

    /** Copia una foto de la galeria a la app y la reduce. Devuelve la ruta, o null si no se pudo leer. */
    suspend fun importFromGallery(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val file = newFile()
            context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            trace("ListingPhotoStore.shrink") { shrink(file) }
        }.getOrNull()
    }

    suspend fun readBytes(path: String): ByteArray? = withContext(Dispatchers.IO) {
        File(path).takeIf { it.exists() }?.readBytes()
    }

    fun delete(paths: List<String>) = paths.forEach { File(it).delete() }

    private fun newFile() = File(folder, "photo-${System.currentTimeMillis()}.jpg")

    private fun shrink(file: File): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val longSide = maxOf(bounds.outWidth, bounds.outHeight)
        if (longSide <= 0) return null

        val options = BitmapFactory.Options().apply {
            var sample = 1
            while (longSide / (sample * 2) >= MAX_SIDE) sample *= 2
            inSampleSize = sample
            val sampledSide = longSide / sample
            if (sampledSide > MAX_SIDE) {
                // El decodificador escala de una vez al tamaño final: no queda un bitmap intermedio grande
                inScaled = true
                inDensity = sampledSide
                inTargetDensity = MAX_SIDE
            }
        }
        val decoded = BitmapFactory.decodeFile(file.path, options) ?: return null

        val rotation = rotationOf(file)
        val result = if (rotation == 0) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
        if (result !== decoded) decoded.recycle()
        file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        result.recycle()
        return file.absolutePath
    }

    private fun rotationOf(file: File): Int = when (
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val QUALITY = 85
    }
}