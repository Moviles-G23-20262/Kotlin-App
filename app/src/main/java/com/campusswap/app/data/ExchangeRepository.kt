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
            materialId = SeedIds.material(target.productId) ?: target.productId.takeIf(::isBackendId) ?: return null,
            buyerId = SeedIds.user(target.buyerId) ?: target.buyerId.takeIf(::isBackendId) ?: return null,
            sellerId = SeedIds.user(target.sellerId) ?: target.sellerId.takeIf(::isBackendId) ?: return null,
            price = String.format(Locale.US, "%.2f", target.price),
            meetingPointId = target.meetingPointId?.let { SeedIds.meetingPoint(it) ?: it },
            lat = location?.lat,
            lng = location?.lng,
        )
    }
}

// Detecta si un ID es un UUID valido (formato backend) para evitar enviar IDs de semillas al backend
private fun isBackendId(id: String) =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(id)
