package com.campusswap.app.data.ratings

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save

sealed interface RatingResult {
    data object Saved : RatingResult
    data object ExchangeNotRecorded : RatingResult
    data object Offline : RatingResult
    data class Rejected(val httpCode: Int) : RatingResult
    /** Saved on the phone; the outbox sends it when there's a connection. */
    data object Queued : RatingResult
    /** The outbox tried to send it and the server said no. */
    data class Refused(val reason: String) : RatingResult
}

data class ExchangeRatings(val mine: RatingDto?, val theirs: RatingDto?)

class RatingRepository(
    private val remote: RatingRemoteDataSource,
    private val cache: ResponseCache = ResponseCache.None,
) {

    suspend fun submit(
        productId: String,
        ratedId: String,
        stars: Int,
        tags: List<String>,
        review: String?,
    ): RatingResult = try {
        val exchangeId = remote.getExchanges(SeedIds.material(productId) ?: productId).firstOrNull()?.id
        if (exchangeId == null) {
            RatingResult.ExchangeNotRecorded
        } else {
            remote.createRating(CreateRatingRequest(exchangeId, ratedId, stars, tags, review))
            RatingResult.Saved
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        RatingResult.Offline
    } catch (e: HttpException) {
        RatingResult.Rejected(e.code())
    }

    /** Lo que dice el backend sobre este intercambio; null si aún no está completado o no hay red. */
    suspend fun exchangeRatings(productId: String, myUserId: String): ExchangeRatings? = try {
        val exchangeId = remote.getExchanges(SeedIds.material(productId) ?: productId).firstOrNull()?.id
        exchangeId?.let { id ->
            val both = remote.getRatings(exchangeId = id)
            ExchangeRatings(
                mine = both.firstOrNull { it.raterId == myUserId },
                theirs = both.firstOrNull { it.raterId != myUserId },
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** Calificaciones que recibe el usuario. Sin red: ultima copia guardada */
    suspend fun received(userId: String): List<RatingDto>? = try {
        remote.getRatings(userId = userId).also { cache.save(userId, RECEIVED_KEY, it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        cache.load<List<RatingDto>>(userId, RECEIVED_KEY)?.value
    }

    private companion object {
        const val RECEIVED_KEY = "ratings-received"
    }
}