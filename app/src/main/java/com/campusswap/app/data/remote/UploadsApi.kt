package com.campusswap.app.data.remote

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadsApi {
    @Multipart
    @POST("uploads")
    suspend fun upload(@Part file: MultipartBody.Part): UploadedImageDto
}

data class UploadedImageDto(val url: String)

interface UploadRemoteDataSource {
    suspend fun upload(bytes: ByteArray, mimeType: String): String
}

class RetrofitUploadRemoteDataSource(private val api: UploadsApi) : UploadRemoteDataSource {
    override suspend fun upload(bytes: ByteArray, mimeType: String): String {
        val body = okhttp3.RequestBody.create(okhttp3.MediaType.parse(mimeType), bytes)
        return api.upload(MultipartBody.Part.createFormData("file", "photo.jpg", body)).url
    }
}
