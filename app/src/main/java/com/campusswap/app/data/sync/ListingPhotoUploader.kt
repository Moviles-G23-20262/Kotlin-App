package com.campusswap.app.data.sync

import com.campusswap.app.data.local.ListingPhotoStore
import com.campusswap.app.data.remote.UploadRemoteDataSource

/** Sube las fotos de una publicacion pendiente y borra las copias locales cuando publica*/
interface ListingPhotoUploader {
    /** URL de la foto subida, o null si el archivo ya no existe en el telefono. */
    suspend fun upload(path: String): String?

    fun discard(paths: List<String>)

    object None : ListingPhotoUploader {
        override suspend fun upload(path: String): String? = null
        override fun discard(paths: List<String>) = Unit
    }
}

class RemoteListingPhotoUploader(
    private val store: ListingPhotoStore,
    private val remote: UploadRemoteDataSource,
) : ListingPhotoUploader {

    override suspend fun upload(path: String): String? {
        val bytes = store.readBytes(path) ?: return null
        return remote.upload(bytes, "image/jpeg")
    }

    override fun discard(paths: List<String>) = store.delete(paths)
}