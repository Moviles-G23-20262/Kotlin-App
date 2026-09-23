package com.campusswap.app.screens.meeting

import com.campusswap.app.data.FakeMeetingPointRemoteDataSource
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.location.CounterpartLocationSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.LocationResult
import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.domain.GeoPoint
import com.campusswap.app.domain.RankingMode
import com.campusswap.app.domain.TimeOfDayStrategySelector
import com.campusswap.app.domain.north
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MeetingPointViewModelTest {
    private val me = GeoPoint(0.0, 0.0)
    private val seller = north(me, 600.0)
    private val remote = FakeMeetingPointRemoteDataSource(
        listOf(
            dto("near-me", monitored = true, meters = 50.0),
            dto("middle-plaza", monitored = false, meters = 300.0),
            dto("near-seller", monitored = true, meters = 450.0),
        ),
    )
    private val location = FakeLocation(LocationResult.Fix(me, accuracyMeters = 5f))

    private class FakeLocation(var result: LocationResult) : LocationDataSource {
        override suspend fun currentLocation() = result
    }

    private fun dto(id: String, monitored: Boolean, meters: Double): MeetingPointDto {
        val at = north(me, meters)
        return MeetingPointDto(id, id, null, "PLAZA", monitored, at.lat, at.lng)
    }

    private fun clockAt(hour: Int) = Clock.fixed(Instant.parse("2026-09-23T%02d:00:00Z".format(hour)), ZoneOffset.UTC)

    private fun viewModel(hour: Int) = MeetingPointViewModel(
        productId = "p1",
        meetingPoints = MeetingPointRepository(remote),
        myLocation = location,
        counterpart = object : CounterpartLocationSource {
            override suspend fun locationOf(productId: String) = seller
        },
        strategies = TimeOfDayStrategySelector(),
        clock = clockAt(hour),
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun afternoonRecommendsTheFairestZoneEvenIfUnmonitored() {
        val state = viewModel(hour = 14).state.value

        assertEquals(RankingMode.DAYTIME, state.mode)
        assertEquals(LocalTime.of(14, 0), state.time)
        assertEquals("middle-plaza", state.recommended?.point?.id)
        assertEquals(6, state.recommended?.walkMinutesMe)
        assertEquals(MyLocationStatus.FOUND, state.myLocation)
        assertTrue(state.isLive)
    }

    @Test fun nightRecommendsOnlyMonitoredZones() {
        val state = viewModel(hour = 22).state.value

        assertEquals(RankingMode.NIGHT_SAFETY, state.mode)
        assertEquals("near-seller", state.recommended?.point?.id)
        assertTrue(state.ranked.all { it.point.isMonitored })
    }

    @Test fun withoutPermissionRanksByTheSellersWalkAndAsksForLocation() {
        location.result = LocationResult.PermissionDenied

        val state = viewModel(hour = 14).state.value

        assertEquals(MyLocationStatus.PERMISSION_NEEDED, state.myLocation)
        assertEquals("near-seller", state.recommended?.point?.id)
        assertNull(state.recommended?.walkMinutesMe)
        assertNull(state.myMapPosition)
    }

    @Test fun refreshAfterGrantingPermissionIncludesMyWalk() {
        location.result = LocationResult.PermissionDenied
        val vm = viewModel(hour = 14)

        location.result = LocationResult.Fix(me, accuracyMeters = 5f)
        vm.refresh()

        assertEquals(MyLocationStatus.FOUND, vm.state.value.myLocation)
        assertEquals("middle-plaza", vm.state.value.recommended?.point?.id)
    }

    @Test fun positionFarFromCampusIsIgnored() {
        location.result = LocationResult.Fix(north(me, 5_000.0), accuracyMeters = 5f)

        val state = viewModel(hour = 14).state.value

        assertEquals(MyLocationStatus.OFF_CAMPUS, state.myLocation)
        assertNull(state.recommended?.walkMinutesMe)
    }

    @Test fun offlineStillSuggestsFromBundledPoints() {
        remote.failure = IOException("offline")

        val state = viewModel(hour = 14).state.value

        assertFalse(state.isLive)
        assertTrue(state.ranked.isNotEmpty())
    }
}
