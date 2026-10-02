package com.campusswap.app.data.ratings

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

sealed interface RatingResult {
    data object Saved : RatingResult
    data object ExchangeNotRecorded : RatingResult
    data object Offline : RatingResult
    data class Rejected(val httpCode: Int) : RatingResult
}

class RatingRepository(private val remote: RatingRemoteDataSource) {

    suspend fun submit(
        productId: String,
        raterId: String,
        ratedId: String,
        stars: Int,
        tags: List<String>,
        review: String?,
    ): RatingResult = try {
        val exchangeId = remote.getExchanges().firstOrNull { it.materialId == productId }?.id
        if (exchangeId == null) {
            RatingResult.ExchangeNotRecorded
        } else {
            remote.createRating(CreateRatingRequest(exchangeId, raterId, ratedId, stars, tags, review))
            RatingResult.Saved
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        RatingResult.Offline
    } catch (e: HttpException) {
        RatingResult.Rejected(e.code())
    }
}