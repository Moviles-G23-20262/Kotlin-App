package com.campusswap.app.model

import com.campusswap.app.model.remote.MeetingPointDto
import com.campusswap.app.model.remote.MeetingPointRemoteDataSource

class FakeMeetingPointRemoteDataSource(var points: List<MeetingPointDto> = emptyList()) : MeetingPointRemoteDataSource {
    var failure: Exception? = null

    override suspend fun meetingPoints(): List<MeetingPointDto> {
        failure?.let { throw it }
        return points
    }
}
