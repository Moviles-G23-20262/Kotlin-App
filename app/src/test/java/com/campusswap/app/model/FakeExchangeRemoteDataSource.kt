package com.campusswap.app.data

import com.campusswap.app.data.remote.ExchangeDto
import com.campusswap.app.data.remote.ExchangeRemoteDataSource

data class Completion(val exchangeId: String, val lat: Double?, val lng: Double?)

class FakeExchangeRemoteDataSource : ExchangeRemoteDataSource {
    val exchanges = mutableListOf<ExchangeDto>()
    val orders = mutableListOf<String>()
    val completions = mutableListOf<Completion>()
    var failure: Exception? = null
    var buyerId = "a0000000-0000-4000-8000-000000000000"

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
        return exchanges[index].copy(status = "COMPLETED").also { exchanges[index] = it }
    }
}
