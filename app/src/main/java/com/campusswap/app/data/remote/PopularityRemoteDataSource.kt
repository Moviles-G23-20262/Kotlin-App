package com.campusswap.app.data.remote

interface PopularityRemoteDataSource {
    suspend fun usageAt(hour: Int): MeetingPointUsageResponse
}

class RetrofitPopularityRemoteDataSource(private val api: AnalyticsApi) : PopularityRemoteDataSource {
    override suspend fun usageAt(hour: Int): MeetingPointUsageResponse = api.meetingPointUsage(hour)
}
