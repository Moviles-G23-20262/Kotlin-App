package com.campusswap.app.model.wishlist

import com.campusswap.app.model.materials.MaterialDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** Copia del JSON de /wishlist-items, backend incluye el material completo (con su vendedor) */
data class WishlistItemDto(
    val id: String,
    val userId: String,
    val materialId: String,
    val material: MaterialDto?,
    val createdAt: String?,
)

/** Sin userId: el backend toma el token (cada usuario guarda para el mismo). */
data class CreateWishlistItemRequest(val materialId: String)

interface WishlistApi {
    @GET("wishlist-items")
    suspend fun getAll(): List<WishlistItemDto>

    @POST("wishlist-items")
    suspend fun add(@Body body: CreateWishlistItemRequest): WishlistItemDto

    @DELETE("wishlist-items/{id}")
    suspend fun remove(@Path("id") itemId: String)
}

interface WishlistRemoteDataSource {
    suspend fun getAll(): List<WishlistItemDto>
    suspend fun add(materialId: String): WishlistItemDto
    suspend fun remove(itemId: String)
}

class RetrofitWishlistRemoteDataSource(private val api: WishlistApi) : WishlistRemoteDataSource {
    override suspend fun getAll() = api.getAll()
    override suspend fun add(materialId: String) = api.add(CreateWishlistItemRequest(materialId))
    override suspend fun remove(itemId: String) = api.remove(itemId)
}