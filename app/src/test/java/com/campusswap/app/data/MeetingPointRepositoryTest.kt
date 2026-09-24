package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MeetingPointRepositoryTest {
    private val remote = FakeMeetingPointRemoteDataSource()
    private val repository = MeetingPointRepository(remote)

    @Test fun mapsBackendPointsKeepingTheirUuid() = runBlocking {
        remote.points = listOf(MeetingPointDto("c0000000-0000-4000-8000-000000000001", "Library", null, "LIBRARY", true, 4.6, -74.06))

        val result = repository.load()

        assertTrue(result.isLive)
        val point = result.points.single()
        assertEquals("c0000000-0000-4000-8000-000000000001", point.id)
        assertEquals(MeetingZoneType.LIBRARY, point.zoneType)
        assertEquals("", point.detail)
        assertEquals(GeoPoint(4.6, -74.06), point.location)
    }

    @Test fun unknownZoneTypeDoesNotBreakParsing() = runBlocking {
        remote.points = listOf(MeetingPointDto("id", "New hall", "x", "ROOFTOP", false, 4.6, -74.06))

        assertEquals(MeetingZoneType.BUILDING_LOBBY, repository.load().points.single().zoneType)
    }

    @Test fun offlineFallsBackToBundledPointsWithCoordinates() = runBlocking {
        remote.failure = IOException("airplane mode")

        val result = repository.load()

        assertFalse(result.isLive)
        assertTrue(result.points.isNotEmpty())
        assertTrue(result.points.all { it.location != null })
    }
}
