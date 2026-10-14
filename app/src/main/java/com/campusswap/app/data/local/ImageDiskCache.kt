package com.campusswap.app.data.local

import java.io.File
import java.security.MessageDigest

/**
 * Segundo nivel del cache de imagenes: los bytes descargados quedan en cacheDir, asi las fotos
 * se ven sin conexion aunque la app se haya cerrado. Si pasa de [maxBytes] borra las que hace
 * mas tiempo no se usan (LRU por lastModified, que se actualiza en cada lectura).
 */
class ImageDiskCache(
    private val folder: File,
    private val maxBytes: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {

    @Synchronized
    fun get(url: String): ByteArray? {
        val file = fileFor(url)
        if (!file.exists()) return null
        file.setLastModified(now())
        return runCatching { file.readBytes() }.getOrNull()
    }

    @Synchronized
    fun put(url: String, bytes: ByteArray) {
        if (bytes.size > maxBytes) return
        folder.mkdirs()
        val target = fileFor(url)
        // Se escribe aparte y se renombra: si la app muere a mitad, no queda una imagen corrupta
        val tmp = File(folder, "${target.name}.tmp")
        val saved = runCatching { tmp.writeBytes(bytes); tmp.renameTo(target) }.getOrDefault(false)
        if (!saved) {
            tmp.delete()
            return
        }
        target.setLastModified(now())
        trim()
    }

    @Synchronized
    fun sizeBytes(): Long = entries().sumOf { it.length() }

    private fun trim() {
        var total = sizeBytes()
        for (file in entries().sortedBy { it.lastModified() }) {
            if (total <= maxBytes) break
            total -= file.length()
            file.delete()
        }
    }

    private fun entries(): List<File> =
        folder.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }.orEmpty()

    private fun fileFor(url: String) = File(folder, sha256(url))

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
