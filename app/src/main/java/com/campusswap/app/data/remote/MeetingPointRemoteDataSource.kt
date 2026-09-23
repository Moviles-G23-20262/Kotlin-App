package com.campusswap.app.data.remote

/** Throws IOException when the backend is unreachable and HttpException when it answers with an error. */
interface MeetingPointRemoteDataSource {
    suspend fun meetingPoints(): List<MeetingPointDto>
}

class RetrofitMeetingPointRemoteDataSource(private val api: CampusSwapApi) : MeetingPointRemoteDataSource {
    override suspend fun meetingPoints(): List<MeetingPointDto> = api.meetingPoints()
}
