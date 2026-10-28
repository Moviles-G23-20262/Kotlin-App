package com.campusswap.app.model

import com.campusswap.app.model.sync.ListingPhotoUploader

class FakePhotoUploader : ListingPhotoUploader {
    var failure: Exception? = null
    val uploaded = mutableListOf<String>()
    val discarded = mutableListOf<String>()

    override suspend fun upload(path: String): String {
        failure?.let { throw it }
        uploaded += path
        return "https://blob.campusswap.test/photo-${uploaded.size}.jpg"
    }

    override fun discard(paths: List<String>) {
        discarded += paths
    }
}
