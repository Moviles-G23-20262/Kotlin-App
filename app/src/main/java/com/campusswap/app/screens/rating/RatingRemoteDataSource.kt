package com.campusswap.app.data.ratings

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.campusswap.app.data.materials.MaterialSellerDto

data class CreateRatingRequest(
    val exchangeId: String,
    val ratedId: String,
    val stars: Int,
    val tags: List<String>,
    val review: String?,
)

data class RatingResponse(val id: String)

data class ExchangeSummaryDto(val id: String, val materialId: String)

data class RatingDto(
    val id: String,
    val exchangeId: String,
    val raterId: String,
    val ratedId: String,
    val stars: Int,
    val tags: List<String>?,
    val review: String?,
    val createdAt: String?,
    val rater: MaterialSellerDto?,
)

interface RatingRemoteDataSource {
    suspend fun getExchanges(materialId: String): List<ExchangeSummaryDto>
    suspend fun createRating(request: CreateRatingRequest): String
    suspend fun getRatings(exchangeId: String? = null, userId: String? = null): List<RatingDto>
}

interface RatingsApi {
    @GET("exchanges")
    suspend fun getExchanges(@Query("materialId") materialId: String, @Query("status") status: String): List<ExchangeSummaryDto>

    @POST("ratings")
    suspend fun createRating(@Body body: CreateRatingRequest): RatingResponse

    @GET("ratings")
    suspend fun getRatings(@Query("exchangeId") exchangeId: String?, @Query("userId") userId: String?): List<RatingDto>
}

class RetrofitRatingRemoteDataSource(private val api: RatingsApi) : RatingRemoteDataSource {
    override suspend fun getExchanges(materialId: String) = api.getExchanges(materialId, "COMPLETED")
    override suspend fun createRating(request: CreateRatingRequest) = api.createRating(request).id
    override suspend fun getRatings(exchangeId: String?, userId: String?) = api.getRatings(exchangeId, userId)
}