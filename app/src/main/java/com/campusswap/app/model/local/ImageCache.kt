package com.campusswap.app.model.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache de fotos en dos niveles:
 * 1. Memoria: LruCache con 1/8 del heap, para que al hacer scroll las fotos no se vuelvan a decodificar.
 * 2. Disco: [ImageDiskCache], para verlas sin conexion despues de cerrar la app.
 * Solo si ninguno la tiene se descarga. Las fotos locales (file://) solo pasan por memoria.
 */
class ImageCache(
    private val disk: ImageDiskCache,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val memory = object : LruCache<String, Bitmap>(memoryBudgetKb()) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    /** Descargas en curso: si dos tarjetas piden la misma foto, se descarga una sola vez */
    private val inFlight = ConcurrentHashMap<String, Deferred<Bitmap?>>()

    fun cached(url: String): ImageBitmap? = memory.get(url)?.asImageBitmap()

    suspend fun load(url: String): ImageBitmap? {
        memory.get(url)?.let { return it.asImageBitmap() }
        val job = inFlight.computeIfAbsent(url) { scope.async(start = CoroutineStart.LAZY) { fetch(url) } }
        return try {
            job.await()?.asImageBitmap()
        } finally {
            inFlight.remove(url, job)
        }
    }

    /** La app paso a segundo plano: se suelta la mitad de la memoria, el disco se queda */
    fun trimMemory() = memory.trimToSize(memory.maxSize() / 2)

    private fun fetch(url: String): Bitmap? {
        val local = url.startsWith("file://")
        val stored = if (local) null else disk.get(url)
        val bytes = stored ?: if (local) readLocal(url) else download(url)
        val bitmap = bytes?.let(::decode) ?: return null
        // Solo se guarda en disco lo que si era una imagen (no una pagina de error)
        if (!local && stored == null) disk.put(url, bytes)
        memory.put(url, bitmap)
        return bitmap
    }

    private fun readLocal(url: String): ByteArray? =
        runCatching { File(url.removePrefix("file://")).readBytes() }.getOrNull()

    private fun download(url: String): ByteArray? = runCatching {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) null
            else connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    /** Reduce la foto a [MAX_SIDE] px de lado para no guardar en memoria fotos de 12 MP */
    private fun decode(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_SIDE = 1080

        fun memoryBudgetKb() = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    }
}
