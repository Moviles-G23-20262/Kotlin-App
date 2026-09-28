package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingPointRemoteDataSource

class FakeMeetingPointRemoteDataSource(var points: List<MeetingPointDto> = emptyList()) : MeetingPointRemoteDataSource {
    var failure: Exception? = null

    override suspend fun meetingPoints(): List<MeetingPointDto> {
        failure?.let { throw it }
        return points
    }
}
