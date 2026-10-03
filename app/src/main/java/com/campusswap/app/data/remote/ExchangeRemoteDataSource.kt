package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ExchangesApi {
    @GET("exchanges")
    suspend fun exchanges(@Query("materialId") materialId: String): List<ExchangeDto>

    @POST("exchanges/orders")
    suspend fun placeOrder(@Body body: PlaceOrderRequest): ExchangeDto

    @POST("exchanges/{id}/complete")
    suspend fun complete(@Path("id") id: String, @Body body: CompleteExchangeRequest): ExchangeDto
}

data class ExchangeDto(val id: String, val buyerId: String, val status: String)

data class PlaceOrderRequest(val materialId: String)

data class CompleteExchangeRequest(val lat: Double?, val lng: Double?)

interface ExchangeRemoteDataSource {
    suspend fun exchangesFor(materialId: String): List<ExchangeDto>
    suspend fun placeOrder(materialId: String): ExchangeDto
    suspend fun complete(exchangeId: String, lat: Double?, lng: Double?): ExchangeDto
}

class RetrofitExchangeRemoteDataSource(private val api: ExchangesApi) : ExchangeRemoteDataSource {
    override suspend fun exchangesFor(materialId: String) = api.exchanges(materialId)
    override suspend fun placeOrder(materialId: String) = api.placeOrder(PlaceOrderRequest(materialId))
    override suspend fun complete(exchangeId: String, lat: Double?, lng: Double?) =
        api.complete(exchangeId, CompleteExchangeRequest(lat, lng))
}
