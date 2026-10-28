package com.campusswap.app.model.remote

interface MeetingPointRemoteDataSource {
    suspend fun meetingPoints(): List<MeetingPointDto>
}

class RetrofitMeetingPointRemoteDataSource(private val api: CampusSwapApi) : MeetingPointRemoteDataSource {
    override suspend fun meetingPoints(): List<MeetingPointDto> = api.meetingPoints()
}
