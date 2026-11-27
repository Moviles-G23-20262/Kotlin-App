package com.campusswap.app.model

import com.campusswap.app.model.remote.ExchangeDto
import com.campusswap.app.model.remote.ExchangeRemoteDataSource
import com.campusswap.app.model.domain.ExchangeTarget
import com.campusswap.app.model.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException

sealed interface ConfirmResult {
    /** Both sides confirmed: the exchange is COMPLETED and the listing SOLD. */
    data object Completed : ConfirmResult
    /** This side's confirmation is recorded; the sale closes when the other side confirms too. */
    data object WaitingForOther : ConfirmResult
    data object Offline : ConfirmResult
    data object NotSynced : ConfirmResult
    /** The seller has no order to confirm: the buyer hasn't ordered (or confirmed at the meetup) yet. */
    data object NoOrder : ConfirmResult
    data class Rejected(val httpCode: Int) : ConfirmResult
}

/** Where the exchange stands on the server, for the check-in screen. */
enum class ExchangeProgress { NOT_CONFIRMED, WAITING_FOR_OTHER, COMPLETED, UNKNOWN }

/**
 * The meetup is confirmed by both people: the buyer once the item is checked, the seller once it is
 * handed over. Each call records the signed-in user's side; the server closes the sale when both did.
 */
class ExchangeRepository(private val remote: ExchangeRemoteDataSource) {
    private val completedProductIds = mutableSetOf<String>()

    /** Both sides confirmed (known in this session). */
    fun isConfirmed(productId: String): Boolean = productId in completedProductIds

    suspend fun confirm(target: ExchangeTarget, location: GeoPoint?): ConfirmResult {
        if (isConfirmed(target.productId)) return ConfirmResult.Completed
        val materialId = SeedIds.backendMaterial(target.productId) ?: return ConfirmResult.NotSynced
        return try {
            val mine = myExchanges(target, materialId)
            if (mine.any { it.status == COMPLETED }) return completed(target)
            val pending = mine.firstOrNull { it.status == PENDING }
            // The buyer may reach the meetup with a deal agreed only in the chat: confirming orders it.
            // The seller never orders their own listing.
            val order = pending ?: if (target.asSeller) return ConfirmResult.NoOrder else remote.placeOrder(materialId)
            val updated = if (order.confirmedBy(target.asSeller)) order else remote.complete(order.id, location?.lat, location?.lng)
            if (updated.status == COMPLETED) completed(target) else ConfirmResult.WaitingForOther
        } catch (e: IOException) {
            ConfirmResult.Offline
        } catch (e: HttpException) {
            ConfirmResult.Rejected(e.code())
        }
    }

    /** Asks the server how far the exchange got, e.g. whether the other side confirmed meanwhile. */
    suspend fun progress(target: ExchangeTarget): ExchangeProgress {
        if (isConfirmed(target.productId)) return ExchangeProgress.COMPLETED
        val materialId = SeedIds.backendMaterial(target.productId) ?: return ExchangeProgress.UNKNOWN
        return try {
            val mine = myExchanges(target, materialId)
            when {
                mine.any { it.status == COMPLETED } -> {
                    completed(target)
                    ExchangeProgress.COMPLETED
                }
                mine.any { it.status == PENDING && it.confirmedBy(target.asSeller) } -> ExchangeProgress.WAITING_FOR_OTHER
                else -> ExchangeProgress.NOT_CONFIRMED
            }
        } catch (e: IOException) {
            ExchangeProgress.UNKNOWN
        } catch (e: HttpException) {
            ExchangeProgress.UNKNOWN
        }
    }

    /** The exchanges of this listing where the signed-in user is on the [target]'s side. */
    private suspend fun myExchanges(target: ExchangeTarget, materialId: String): List<ExchangeDto> {
        val all = remote.exchangesFor(materialId)
        return if (target.asSeller) {
            val sellerId = SeedIds.backendUser(target.sellerId)
            all.filter { sellerId == null || it.sellerId == null || it.sellerId == sellerId }
        } else {
            val buyerId = SeedIds.backendUser(target.buyerId)
            all.filter { buyerId == null || it.buyerId == buyerId }
        }
    }

    private fun ExchangeDto.confirmedBy(seller: Boolean) = (if (seller) sellerConfirmedAt else buyerConfirmedAt) != null

    private fun completed(target: ExchangeTarget): ConfirmResult {
        completedProductIds += target.productId
        return ConfirmResult.Completed
    }

    private companion object {
        const val PENDING = "PENDING"
        const val COMPLETED = "COMPLETED"
    }
}
