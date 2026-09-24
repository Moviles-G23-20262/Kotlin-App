package com.campusswap.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("analytics/meeting-points/")
    suspend fun meetingPointUsage(@Query("hour") hour: Int): MeetingPointUsageResponse
}

data class MeetingPointUsageResponse(
    val available: Boolean,
    val data: List<MeetingPointUsageDto>?,
)

data class MeetingPointUsageDto(
    @SerializedName("meeting_point_id") val meetingPointId: String,
    val total: Int,
)
