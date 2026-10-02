package com.campusswap.app.data.ratings

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class CreateRatingRequest(
    val exchangeId: String,
    val raterId: String,
    val ratedId: String,
    val stars: Int,
    val tags: List<String>,
    val review: String?,
)

data class RatingResponse(val id: String)

data class ExchangeSummaryDto(val id: String, val materialId: String)

interface RatingRemoteDataSource {
    suspend fun getExchanges(): List<ExchangeSummaryDto>
    suspend fun createRating(request: CreateRatingRequest): String
}

interface RatingsApi {
    @GET("exchanges")
    suspend fun getExchanges(): List<ExchangeSummaryDto>

    @POST("ratings")
    suspend fun createRating(@Body body: CreateRatingRequest): RatingResponse
}

class RetrofitRatingRemoteDataSource(private val api: RatingsApi) : RatingRemoteDataSource {
    override suspend fun getExchanges() = api.getExchanges()
    override suspend fun createRating(request: CreateRatingRequest) = api.createRating(request).id
}