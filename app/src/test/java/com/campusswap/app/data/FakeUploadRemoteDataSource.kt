package com.campusswap.app.data

import com.campusswap.app.data.remote.UploadRemoteDataSource

class FakeUploadRemoteDataSource : UploadRemoteDataSource {
    var failure: Exception? = null
    val uploaded = mutableListOf<Pair<Int, String>>()

    override suspend fun upload(bytes: ByteArray, mimeType: String): String {
        failure?.let { throw it }
        uploaded += bytes.size to mimeType
        return "https://blob.campusswap.test/photo-${uploaded.size}.jpg"
    }
}
