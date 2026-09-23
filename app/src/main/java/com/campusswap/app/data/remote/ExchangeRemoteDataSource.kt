package com.campusswap.app.data.remote

/** Throws IOException when the backend is unreachable and HttpException when it rejects the request. */
interface ExchangeRemoteDataSource {
    suspend fun createExchange(request: CreateExchangeRequest): String
}

class RetrofitExchangeRemoteDataSource(private val api: CampusSwapApi) : ExchangeRemoteDataSource {
    override suspend fun createExchange(request: CreateExchangeRequest): String = api.createExchange(request).id
}
