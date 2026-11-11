package com.campusswap.app.model.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface AnalyticsEventsApi {
    /**
     * Stores up to 100 events in one request. Idempotent: an event sent twice (a lost response, a
     * retry after a crash) is stored once, because the server keys it on [AnalyticsEventBatchItem.clientId].
     */
    @POST("analytics-events/batch")
    suspend fun createBatch(@Body body: AnalyticsEventBatchRequest): AnalyticsEventBatchResult
}

data class AnalyticsEventBatchRequest(val events: List<AnalyticsEventBatchItem>)

data class AnalyticsEventBatchItem(
    /** UUID made on the phone, the same every time this event is sent. */
    @SerializedName("client_id") val clientId: String,
    val eventType: String,
    val materialId: String?,
    val metadata: Map<String, Any?>,
    val occurredAt: String,
    /** When the phone created the event, even if it reaches the server much later. */
    val clientCreatedAt: String,
)

data class AnalyticsEventBatchResult(
    val received: Int = 0,
    val created: Int = 0,
    val duplicates: Int = 0,
    val rejected: List<RejectedAnalyticsEvent> = emptyList(),
)

data class RejectedAnalyticsEvent(val index: Int, @SerializedName("client_id") val clientId: String?, val reason: String)
