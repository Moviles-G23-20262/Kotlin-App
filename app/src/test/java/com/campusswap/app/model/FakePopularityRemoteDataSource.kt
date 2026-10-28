package com.campusswap.app.model

import com.campusswap.app.model.remote.MeetingPointUsageResponse
import com.campusswap.app.model.remote.PopularityRemoteDataSource

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
