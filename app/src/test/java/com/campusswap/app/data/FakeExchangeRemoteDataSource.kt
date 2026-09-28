package com.campusswap.app.data

import com.campusswap.app.data.remote.CreateExchangeRequest
import com.campusswap.app.data.remote.ExchangeRemoteDataSource

class FakeExchangeRemoteDataSource : ExchangeRemoteDataSource {
    val requests = mutableListOf<CreateExchangeRequest>()
    var failure: Exception? = null

    override suspend fun createExchange(request: CreateExchangeRequest): String {
        failure?.let { throw it }
        requests += request
        return "exchange-${requests.size}"
    }
}
