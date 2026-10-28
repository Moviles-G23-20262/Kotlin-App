package com.campusswap.app.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface CampusSwapApi {
    @GET("meeting-points")
    suspend fun meetingPoints(): List<MeetingPointDto>
}

data class MeetingPointDto(
    val id: String,
    val name: String,
    val detail: String?,
    val zoneType: String,
    val isMonitored: Boolean,
    val lat: Double,
    val lng: Double,
)

object ApiClient {
    inline fun <reified T> create(baseUrl: String, client: OkHttpClient = OkHttpClient()): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(T::class.java)
}
