package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsEventsApi {
    @POST("analytics-events")
    suspend fun create(@Body body: AnalyticsEventRequest)
}

data class AnalyticsEventRequest(
    val eventType: String,
    val materialId: String?,
    val metadata: Map<String, Any?>,
    val occurredAt: String,
)
