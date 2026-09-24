package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointUsageResponse
import com.campusswap.app.data.remote.PopularityRemoteDataSource

class FakePopularityRemoteDataSource(var response: MeetingPointUsageResponse = MeetingPointUsageResponse(true, emptyList())) :
    PopularityRemoteDataSource {
    var failure: Exception? = null
    val requestedHours = mutableListOf<Int>()

    override suspend fun usageAt(hour: Int): MeetingPointUsageResponse {
        requestedHours += hour
        failure?.let { throw it }
        return response
    }
}
