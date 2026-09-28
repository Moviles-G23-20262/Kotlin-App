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
    /** The listing only exists on this device (published locally), so the backend has no row for it. */
    data object NotSynced : ConfirmResult
    data class Rejected(val httpCode: Int) : ConfirmResult
}

class ExchangeRepository(private val remote: ExchangeRemoteDataSource) {
    // Exchange.materialId is unique in the backend; remembering confirmations avoids a duplicate POST
    // when the user leaves View 12 and comes back during the same session.
    private val confirmedProductIds = mutableSetOf<String>()

    fun isConfirmed(productId: String): Boolean = productId in confirmedProductIds

    /** [location] is null for a manual check-in; the meeting point is still recorded. */
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
            // Backend validates ^\d+(\.\d{1,2})?$, so never let the device locale insert a comma.
            price = String.format(Locale.US, "%.2f", target.price),
            meetingPointId = target.meetingPointId?.let(SeedIds::meetingPoint),
            lat = location?.lat,
            lng = location?.lng,
        )
    }
}
