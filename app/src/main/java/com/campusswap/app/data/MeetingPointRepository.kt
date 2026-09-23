package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingPointRemoteDataSource
import com.campusswap.app.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException

data class MeetingPoints(val points: List<MeetingPoint>, val isLive: Boolean)

class MeetingPointRepository(private val remote: MeetingPointRemoteDataSource) {

    suspend fun load(): MeetingPoints = try {
        MeetingPoints(remote.meetingPoints().map { it.toMeetingPoint() }, isLive = true)
    } catch (e: IOException) {
        offline()
    } catch (e: HttpException) {
        offline()
    }

    private fun offline() = MeetingPoints(SampleData.meetingPoints.filter { it.location != null }, isLive = false)

    private fun MeetingPointDto.toMeetingPoint() = MeetingPoint(
        id = id,
        name = name,
        detail = detail.orEmpty(),
        zoneType = MeetingZoneType.entries.firstOrNull { it.name == zoneType } ?: MeetingZoneType.BUILDING_LOBBY,
        isMonitored = isMonitored,
        walkMinutesMe = 0,
        walkMinutesOther = 0,
        mapX = 0f,
        mapY = 0f,
        location = GeoPoint(lat, lng),
    )
}
