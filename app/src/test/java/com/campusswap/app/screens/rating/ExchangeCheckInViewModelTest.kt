package com.campusswap.app.screens.rating

import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.FakeExchangeRemoteDataSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.LocationResult
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import com.campusswap.app.domain.north
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeCheckInViewModelTest {
    private val meetingPoint = GeoPoint(10.0, 20.0)
    private val target = ExchangeTarget("p1", "me", "s1", 65000.0, "mp1", meetingPoint)
    private val remote = FakeExchangeRemoteDataSource()
    private val repository = ExchangeRepository(remote)
    private val events = mutableListOf<Pair<String, Map<String, Any?>>>()

    private class FakeLocation(var result: LocationResult) : LocationDataSource {
        override suspend fun currentLocation() = result
    }

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(location: LocationResult, target: ExchangeTarget = this.target) =
        ExchangeCheckInViewModel(target, FakeLocation(location), repository) { name, properties -> events += name to properties }

    private fun fixAt(meters: Double) = LocationResult.Fix(north(meetingPoint, meters), accuracyMeters = 5f)

    @Test fun insideRadiusConfirmsWithCoordinates() {
        val vm = viewModel(fixAt(20.0))

        vm.checkIn()

        assertEquals(CheckInState.Confirmed(verifiedByGps = true), vm.state.value)
        assertEquals(north(meetingPoint, 20.0).lat, remote.requests.single().lat!!, 1e-9)
    }

    @Test fun outsideRadiusReportsDistanceAndSendsNothing() {
        val vm = viewModel(fixAt(120.0))

        vm.checkIn()

        assertEquals(CheckInState.TooFar(120), vm.state.value)
        assertTrue(remote.requests.isEmpty())
    }

    @Test fun deniedPermissionFallsBackToManualWithoutCoordinates() {
        val vm = viewModel(fixAt(0.0))

        vm.onPermissionDenied()
        assertEquals(CheckInState.Manual(ManualReason.PERMISSION_DENIED), vm.state.value)

        vm.confirmManually()
        assertEquals(CheckInState.Confirmed(verifiedByGps = false), vm.state.value)
        assertNull(remote.requests.single().lat)
    }

    @Test fun locationServicesOffFallsBackToManual() {
        val vm = viewModel(LocationResult.ProviderDisabled)

        vm.checkIn()

        assertEquals(CheckInState.Manual(ManualReason.LOCATION_OFF), vm.state.value)
    }

    @Test fun noFixFallsBackToManual() {
        val vm = viewModel(LocationResult.Unavailable)

        vm.checkIn()

        assertEquals(CheckInState.Manual(ManualReason.NO_FIX), vm.state.value)
    }

    @Test fun unmappedMeetingPointStartsInManualMode() {
        val vm = viewModel(fixAt(0.0), target.copy(meetingPoint = null))

        assertEquals(CheckInState.Manual(ManualReason.POINT_NOT_MAPPED), vm.state.value)
    }

    @Test fun retryAfterOfflineResendsTheSameFix() {
        val vm = viewModel(fixAt(10.0))
        remote.failure = IOException("offline")

        vm.checkIn()
        assertEquals(CheckInState.Failed(FailReason.OFFLINE), vm.state.value)

        remote.failure = null
        vm.retry()
        assertEquals(CheckInState.Confirmed(verifiedByGps = true), vm.state.value)
        assertEquals(north(meetingPoint, 10.0).lat, remote.requests.single().lat!!, 1e-9)
    }

    @Test fun exchangeConfirmedEarlierInTheSessionStartsConfirmed() {
        viewModel(fixAt(0.0)).checkIn()

        val reopened = viewModel(fixAt(0.0))

        assertEquals(CheckInState.Confirmed(verifiedByGps = null), reopened.state.value)
    }

    @Test fun gpsConfirmationLogsMethodCoordinatesAndDistance() {
        viewModel(fixAt(20.0)).checkIn()

        val (name, properties) = events.single()
        assertEquals("exchange_confirmed", name)
        assertEquals("gps", properties["method"])
        assertEquals("mp1", properties["meeting_point_id"])
        assertEquals(north(meetingPoint, 20.0).lat, properties["lat"] as Double, 1e-9)
        assertEquals(20, properties["distance_m"])
        assertNull(properties["manual_reason"])
    }

    @Test fun manualConfirmationLogsTheFallbackReason() {
        val vm = viewModel(LocationResult.ProviderDisabled)
        vm.checkIn()

        vm.confirmManually()

        val properties = events.single().second
        assertEquals("manual", properties["method"])
        assertEquals("location_off", properties["manual_reason"])
        assertNull(properties["lat"])
        assertNull(properties["distance_m"])
    }

    @Test fun failedSendIsNotLoggedAsConfirmed() {
        remote.failure = IOException("offline")

        viewModel(fixAt(10.0)).checkIn()

        assertTrue(events.isEmpty())
    }
}
