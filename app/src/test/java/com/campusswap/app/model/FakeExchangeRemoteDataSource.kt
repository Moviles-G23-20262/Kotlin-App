package com.campusswap.app.model

import com.campusswap.app.model.remote.ExchangeDto
import com.campusswap.app.model.remote.ExchangeRemoteDataSource

data class Completion(val exchangeId: String, val lat: Double?, val lng: Double?)

/**
 * Behaves like the backend: POST /exchanges/:id/complete records the caller's side, and the exchange
 * is COMPLETED only once the buyer and the seller have both confirmed.
 */
class FakeExchangeRemoteDataSource : ExchangeRemoteDataSource {
    val exchanges = mutableListOf<ExchangeDto>()
    val orders = mutableListOf<String>()
    val completions = mutableListOf<Completion>()
    var failure: Exception? = null
    var buyerId = "a0000000-0000-4000-8000-000000000000"
    /** Who is signed in: the seller confirms the handover, the buyer the reception. */
    var callerIsSeller = false
    /** The other side already confirmed on the server, so this side's confirmation closes the sale. */
    var otherSideConfirmed = false

    override suspend fun exchangesFor(materialId: String): List<ExchangeDto> {
        failure?.let { throw it }
        return exchanges.toList()
    }

    override suspend fun placeOrder(materialId: String): ExchangeDto {
        failure?.let { throw it }
        orders += materialId
        return ExchangeDto("order-${orders.size}", buyerId, "PENDING").also { exchanges += it }
    }

    override suspend fun complete(exchangeId: String, lat: Double?, lng: Double?): ExchangeDto {
        failure?.let { throw it }
        completions += Completion(exchangeId, lat, lng)
        val index = exchanges.indexOfFirst { it.id == exchangeId }
        var exchange = exchanges[index]
        val now = "2026-11-22T15:00:00Z"
        exchange = if (callerIsSeller) exchange.copy(sellerConfirmedAt = exchange.sellerConfirmedAt ?: now)
        else exchange.copy(buyerConfirmedAt = exchange.buyerConfirmedAt ?: now)
        if (otherSideConfirmed) {
            exchange = if (callerIsSeller) exchange.copy(buyerConfirmedAt = exchange.buyerConfirmedAt ?: now)
            else exchange.copy(sellerConfirmedAt = exchange.sellerConfirmedAt ?: now)
        }
        if (exchange.buyerConfirmedAt != null && exchange.sellerConfirmedAt != null) exchange = exchange.copy(status = "COMPLETED")
        exchanges[index] = exchange
        return exchange
    }

    /** The other person confirms from their phone while this one waits. */
    fun otherSideConfirmsNow() {
        exchanges.replaceAll { e ->
            if (e.status != "PENDING") e else e.copy(status = "COMPLETED", buyerConfirmedAt = "x", sellerConfirmedAt = "x")
        }
    }
}
