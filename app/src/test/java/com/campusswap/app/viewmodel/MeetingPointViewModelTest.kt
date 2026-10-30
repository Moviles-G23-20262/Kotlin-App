package com.campusswap.app.viewmodel

import com.campusswap.app.model.ChatRoomRepository
import com.campusswap.app.model.FakeChatRoomRemoteDataSource
import com.campusswap.app.model.FakeMeetingPointRemoteDataSource
import com.campusswap.app.model.FakeMeetingProposalRemoteDataSource
import com.campusswap.app.model.FakePopularityRemoteDataSource
import com.campusswap.app.model.MeetingPointPopularityRepository
import com.campusswap.app.model.MeetingProposalRepository
import com.campusswap.app.model.MeetingPointRepository
import com.campusswap.app.model.local.InMemoryResponseCache
import com.campusswap.app.model.local.ResponseCache
import com.campusswap.app.model.location.CounterpartLocationSource
import com.campusswap.app.model.location.LocationDataSource
import com.campusswap.app.model.location.LocationResult
import com.campusswap.app.model.remote.MeetingPointDto
import com.campusswap.app.model.remote.MeetingPointUsageDto
import com.campusswap.app.model.remote.MeetingPointUsageResponse
import com.campusswap.app.model.remote.FreeSlotDto
import com.campusswap.app.model.remote.SlotSuggestionsDto
import com.campusswap.app.model.domain.GeoPoint
import com.campusswap.app.model.domain.RankingMode
import com.campusswap.app.model.domain.TimeOfDayStrategySelector
import com.campusswap.app.model.domain.north
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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
    private val proposalRemote = FakeMeetingProposalRemoteDataSource().apply {
        val slot = FreeSlotDto("2026-10-05T17:00:00Z", "2026-10-05T18:00:00Z", sharedBreak = true)
        suggestions = SlotSuggestionsDto(listOf(slot), suggested = slot)
    }
    private val location = FakeLocation(LocationResult.Fix(me, accuracyMeters = 5f))
    private val popularityRemote = FakePopularityRemoteDataSource(
        MeetingPointUsageResponse(true, listOf(MeetingPointUsageDto("near-seller", 6), MeetingPointUsageDto("near-me", 2))),
    )

    private class FakeLocation(var result: LocationResult) : LocationDataSource {
        override suspend fun currentLocation() = result
    }

    private fun dto(id: String, monitored: Boolean, meters: Double): MeetingPointDto {
        val at = north(me, meters)
        return MeetingPointDto(id, id, null, "PLAZA", monitored, at.lat, at.lng)
    }

    private fun clockAt(hour: Int) = Clock.fixed(Instant.parse("2026-09-23T%02d:00:00Z".format(hour)), ZoneOffset.UTC)

    private fun viewModel(hour: Int, popularityCache: ResponseCache = ResponseCache.None) = MeetingPointViewModel(
        productId = "p1",
        meetingPoints = MeetingPointRepository(remote),
        myLocation = location,
        counterpart = object : CounterpartLocationSource {
            override suspend fun locationOf(productId: String) = seller
        },
        strategies = TimeOfDayStrategySelector(),
        clock = clockAt(hour),
        popularity = MeetingPointPopularityRepository(popularityRemote, popularityCache),
        proposals = MeetingProposalRepository(ChatRoomRepository(FakeChatRoomRemoteDataSource()), proposalRemote),
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

    @Test fun marksPointsThatArePopularAtTheCurrentHour() {
        val state = viewModel(hour = 13).state.value

        assertEquals(listOf(13), popularityRemote.requestedHours)
        assertEquals(setOf("near-seller", "near-me"), state.ranked.filter { it.isPopular }.map { it.point.id }.toSet())
        assertFalse(state.ranked.first { it.point.id == "middle-plaza" }.isPopular)
    }

    @Test fun analyticsFailureHidesBadgesWithoutAffectingTheSuggestion() {
        popularityRemote.failure = IOException("analytics down")

        val state = viewModel(hour = 14).state.value

        assertTrue(state.ranked.none { it.isPopular })
        assertEquals("middle-plaza", state.recommended?.point?.id)
    }

    @Test fun liveBadgesAreNotMarkedAsSaved() {
        assertNull(viewModel(hour = 13).state.value.popularSavedAtMillis)
    }

    @Test fun offlineBadgesComeFromTheSavedCopyAndSayHowOldItIs() = runBlocking {
        val cache = InMemoryResponseCache(now = 7_000L)
        MeetingPointPopularityRepository(popularityRemote, cache).popularAt(13)
        popularityRemote.failure = IOException("airplane mode")

        val state = withTimeout(5_000) { viewModel(hour = 13, cache).state.first { it.popularSavedAtMillis != null } }

        assertEquals(setOf("near-seller", "near-me"), state.ranked.filter { it.isPopular }.map { it.point.id }.toSet())
        assertEquals(7_000L, state.popularSavedAtMillis)
    }

    @Test fun showsTheSharedFreeTimesFromTheServer() {
        val state = viewModel(hour = 14).state.value

        assertEquals(listOf("12:00 – 13:00"), state.slots.map { it.label })
    }

    @Test fun freeTimesSurviveTheSecondPublishAfterTheGpsFix() {
        val vm = viewModel(hour = 14)

        vm.refresh()

        assertEquals(1, vm.state.value.slots.size)
        assertEquals(MyLocationStatus.FOUND, vm.state.value.myLocation)
    }
}
