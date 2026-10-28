package com.campusswap.app.model.orders

import com.campusswap.app.model.remote.ExchangeDto
import com.campusswap.app.model.remote.PlaceOrderRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Las órdenes del usuario son Exchanges del backend: PENDING al comprar, COMPLETED tras el check-in */
interface OrdersApi {
    @GET("exchanges")
    suspend fun mine(): List<ExchangeDto>

    @POST("exchanges/orders")
    suspend fun place(@Body body: PlaceOrderRequest): ExchangeDto
}

interface OrdersRemoteDataSource {
    suspend fun mine(): List<ExchangeDto>
    suspend fun place(materialId: String): ExchangeDto
}

class RetrofitOrdersRemoteDataSource(private val api: OrdersApi) : OrdersRemoteDataSource {
    override suspend fun mine() = api.mine()
    override suspend fun place(materialId: String) = api.place(PlaceOrderRequest(materialId))
}