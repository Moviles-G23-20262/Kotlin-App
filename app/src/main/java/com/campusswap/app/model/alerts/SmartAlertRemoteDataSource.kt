package com.campusswap.app.data.alerts

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Copia del JSON de /smart-alerts; maxPrice llega como texto ("45000.00") porque es Decimal. */
data class SmartAlertDto(
    val id: String,
    val keyword: String?,
    val courseCode: String?,
    val category: String?,
    val maxPrice: String,
    val minCondition: String?,
    val enabled: Boolean,
)

/** Sin userId: el backend toma el token. null en category/minCondition = cualquiera. */
data class CreateSmartAlertRequest(
    val keyword: String,
    val courseCode: String?,
    val category: String?,
    val maxPrice: String,
    val minCondition: String?,
    val enabled: Boolean,
)

data class UpdateSmartAlertRequest(val enabled: Boolean)

interface SmartAlertsApi {
    @GET("smart-alerts")
    suspend fun getAll(): List<SmartAlertDto>

    @POST("smart-alerts")
    suspend fun create(@Body body: CreateSmartAlertRequest): SmartAlertDto

    @PATCH("smart-alerts/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateSmartAlertRequest): SmartAlertDto

    @DELETE("smart-alerts/{id}")
    suspend fun remove(@Path("id") id: String)
}

interface SmartAlertRemoteDataSource {
    suspend fun getAll(): List<SmartAlertDto>
    suspend fun create(request: CreateSmartAlertRequest): SmartAlertDto
    suspend fun setEnabled(id: String, enabled: Boolean): SmartAlertDto
    suspend fun remove(id: String)
}

class RetrofitSmartAlertRemoteDataSource(private val api: SmartAlertsApi) : SmartAlertRemoteDataSource {
    override suspend fun getAll() = api.getAll()
    override suspend fun create(request: CreateSmartAlertRequest) = api.create(request)
    override suspend fun setEnabled(id: String, enabled: Boolean) = api.update(id, UpdateSmartAlertRequest(enabled))
    override suspend fun remove(id: String) = api.remove(id)
}
