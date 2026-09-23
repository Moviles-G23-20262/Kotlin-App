package com.campusswap.app.data

import com.campusswap.app.data.remote.CreateExchangeRequest
import com.campusswap.app.data.remote.ExchangeRemoteDataSource
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

sealed interface ConfirmResult {
    data object Success : ConfirmResult
    data object Offline : ConfirmResult
    data object NotSynced : ConfirmResult
    data class Rejected(val httpCode: Int) : ConfirmResult
}

class ExchangeRepository(private val remote: ExchangeRemoteDataSource) {
    private val confirmedProductIds = mutableSetOf<String>()

    fun isConfirmed(productId: String): Boolean = productId in confirmedProductIds

    suspend fun confirm(target: ExchangeTarget, location: GeoPoint?): ConfirmResult {
        if (isConfirmed(target.productId)) return ConfirmResult.Success
        val request = toRequest(target, location) ?: return ConfirmResult.NotSynced
        return try {
            remote.createExchange(request)
            confirmedProductIds += target.productId
            ConfirmResult.Success
        } catch (e: IOException) {
            ConfirmResult.Offline
        } catch (e: HttpException) {
            ConfirmResult.Rejected(e.code())
        }
    }

    private fun toRequest(target: ExchangeTarget, location: GeoPoint?): CreateExchangeRequest? {
        return CreateExchangeRequest(
            materialId = SeedIds.material(target.productId) ?: return null,
            buyerId = SeedIds.user(target.buyerId) ?: return null,
            sellerId = SeedIds.user(target.sellerId) ?: return null,
            price = String.format(Locale.US, "%.2f", target.price),
            // Points loaded from the API already carry their UUID; only bundled SampleData ids need mapping.
            meetingPointId = target.meetingPointId?.let { SeedIds.meetingPoint(it) ?: it },
            lat = location?.lat,
            lng = location?.lng,
        )
    }
}
