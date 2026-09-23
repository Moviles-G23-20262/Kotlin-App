package com.campusswap.app.data.remote

import com.campusswap.app.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CampusSwapApi {
    @POST("exchanges")
    suspend fun createExchange(@Body body: CreateExchangeRequest): ExchangeResponse

    @GET("meeting-points")
    suspend fun meetingPoints(): List<MeetingPointDto>
}

data class CreateExchangeRequest(
    val materialId: String,
    val buyerId: String,
    val sellerId: String,
    val price: String,
    val meetingPointId: String?,
    val lat: Double?,
    val lng: Double?,
)

data class ExchangeResponse(val id: String)

/** Mirrors the MeetingPoint model in back-end; zoneType is kept as text so an unknown value can't crash parsing. */
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
    fun create(baseUrl: String = BuildConfig.BASE_URL): CampusSwapApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CampusSwapApi::class.java)
}
