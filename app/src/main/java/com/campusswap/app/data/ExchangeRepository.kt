package com.campusswap.app.data

import com.campusswap.app.data.remote.ExchangeRemoteDataSource
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException

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
        val materialId = SeedIds.material(target.productId) ?: target.productId.takeIf(::isBackendId) ?: return ConfirmResult.NotSynced
        val buyerId = SeedIds.user(target.buyerId) ?: target.buyerId.takeIf(::isBackendId)
        return try {
            val mine = remote.exchangesFor(materialId).filter { buyerId == null || it.buyerId == buyerId }
            if (mine.none { it.status == COMPLETED }) {
                val order = mine.firstOrNull { it.status == PENDING } ?: remote.placeOrder(materialId)
                remote.complete(order.id, location?.lat, location?.lng)
            }
            confirmedProductIds += target.productId
            ConfirmResult.Success
        } catch (e: IOException) {
            ConfirmResult.Offline
        } catch (e: HttpException) {
            ConfirmResult.Rejected(e.code())
        }
    }

    private companion object {
        const val PENDING = "PENDING"
        const val COMPLETED = "COMPLETED"
    }
}

// Detecta si un ID es un UUID valido (formato backend) para evitar enviar IDs de semillas al backend
private fun isBackendId(id: String) =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").matches(id)
