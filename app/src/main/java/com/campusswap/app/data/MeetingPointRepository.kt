package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingPointRemoteDataSource
import com.campusswap.app.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException

/** [isLive] is false when the backend couldn't be reached and the bundled points are shown instead. */
data class MeetingPoints(val points: List<MeetingPoint>, val isLive: Boolean)

class MeetingPointRepository(private val remote: MeetingPointRemoteDataSource) {

    /** Only points with coordinates are returned: without them walking time can't be computed. */
    suspend fun load(): MeetingPoints = try {
        MeetingPoints(remote.meetingPoints().map { it.toMeetingPoint() }, isLive = true)
    } catch (e: IOException) {
        offline()
    } catch (e: HttpException) {
        offline()
    }

    // The screen must keep working without network, so fall back to the points shipped with the app.
    private fun offline() = MeetingPoints(SampleData.meetingPoints.filter { it.location != null }, isLive = false)

    private fun MeetingPointDto.toMeetingPoint() = MeetingPoint(
        id = id,
        name = name,
        detail = detail.orEmpty(),
        zoneType = MeetingZoneType.entries.firstOrNull { it.name == zoneType } ?: MeetingZoneType.BUILDING_LOBBY,
        isMonitored = isMonitored,
        // Walk times and map position are computed per request by the ranking, not stored.
        walkMinutesMe = 0,
        walkMinutesOther = 0,
        mapX = 0f,
        mapY = 0f,
        location = GeoPoint(lat, lng),
    )
}
