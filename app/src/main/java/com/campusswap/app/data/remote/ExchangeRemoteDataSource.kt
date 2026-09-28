package com.campusswap.app.data.remote

interface ExchangeRemoteDataSource {
    suspend fun createExchange(request: CreateExchangeRequest): String
}

class RetrofitExchangeRemoteDataSource(private val api: CampusSwapApi) : ExchangeRemoteDataSource {
    override suspend fun createExchange(request: CreateExchangeRequest): String = api.createExchange(request).id
}
