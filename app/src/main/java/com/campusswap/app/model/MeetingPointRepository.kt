package com.campusswap.app.model

import com.campusswap.app.model.local.ResponseCache
import com.campusswap.app.model.local.load
import com.campusswap.app.model.local.save
import com.campusswap.app.model.remote.MeetingPointDto
import com.campusswap.app.model.remote.MeetingPointRemoteDataSource
import com.campusswap.app.model.domain.GeoPoint
import retrofit2.HttpException
import java.io.IOException

data class MeetingPoints(val points: List<MeetingPoint>, val isLive: Boolean)

/**
 * Campus meeting points. Offline it uses, in order: the ones loaded during this run, the copy
 * saved on the phone by an earlier run, and finally the built-in list.
 */
class MeetingPointRepository(
    private val remote: MeetingPointRemoteDataSource,
    private val cache: ResponseCache = ResponseCache.None,
) {
    @Volatile
    private var lastLive: List<MeetingPoint> = emptyList()

    suspend fun load(): MeetingPoints = try {
        val dtos = remote.meetingPoints()
        cache.save(ResponseCache.SHARED, KEY, dtos)
        val points = dtos.map { it.toMeetingPoint() }
        lastLive = points
        MeetingPoints(points, isLive = true)
    } catch (e: IOException) {
        offline()
    } catch (e: HttpException) {
        offline()
    }

    private suspend fun offline(): MeetingPoints {
        val points = lastLive.ifEmpty { cache.load<List<MeetingPointDto>>(ResponseCache.SHARED, KEY)?.value?.map { it.toMeetingPoint() }.orEmpty() }
        return MeetingPoints(points.ifEmpty { SampleData.meetingPoints.filter { it.location != null } }, isLive = false)
    }

    private companion object {
        const val KEY = "meeting-points"
    }
}

internal fun MeetingPointDto.toMeetingPoint() = MeetingPoint(
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
