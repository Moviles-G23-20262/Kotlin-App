package com.campusswap.app.data.materials

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

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

data class CreateMaterialRequest(
    val title: String,
    val description: String,
    val courseCode: String?,
    val price: String,
    val condition: String,
    val category: String,
    val imageUrls: List<String> = emptyList(),
)

interface MaterialRemoteDataSource {
    suspend fun getMaterials(): List<MaterialDto>
    suspend fun create(request: CreateMaterialRequest): MaterialDto
}

interface MaterialsApi {
    @GET("materials")
    suspend fun getMaterials(): List<MaterialDto>

    @POST("materials")
    suspend fun create(@Body body: CreateMaterialRequest): MaterialDto
}

class RetrofitMaterialRemoteDataSource(private val api: MaterialsApi) : MaterialRemoteDataSource {
    override suspend fun getMaterials() = api.getMaterials()
    override suspend fun create(request: CreateMaterialRequest) = api.create(request)
}