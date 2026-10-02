package com.campusswap.app.data.materials

import retrofit2.http.GET

data class MaterialSellerDto(
    val id: String,
    val email: String,
    val fullName: String,
    val rating: Double?,
)

data class MaterialDto(
    val id: String,
    val title: String,
    val description: String,
    val courseCode: String?,
    val price: String,
    val condition: String?,
    val status: String,
    val category: String,
    val sellerId: String,
    val seller: MaterialSellerDto? = null,
)

interface MaterialRemoteDataSource {
    suspend fun getMaterials(): List<MaterialDto>
}

interface MaterialsApi {
    @GET("materials")
    suspend fun getMaterials(): List<MaterialDto>
}

class RetrofitMaterialRemoteDataSource(private val api: MaterialsApi) : MaterialRemoteDataSource {
    override suspend fun getMaterials() = api.getMaterials()
}