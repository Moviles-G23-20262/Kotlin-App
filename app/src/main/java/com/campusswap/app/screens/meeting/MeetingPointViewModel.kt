package com.campusswap.app.screens.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.data.MeetingPoint
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.MeetingPoints
import com.campusswap.app.data.location.CounterpartLocationSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.LocationResult
import com.campusswap.app.domain.GeoPoint
import com.campusswap.app.domain.MeetingPointRankingStrategy
import com.campusswap.app.domain.Proximity
import com.campusswap.app.domain.RankingMode
import com.campusswap.app.domain.RankingStrategySelector
import com.campusswap.app.domain.ZoneCandidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class MyLocationStatus { LOCATING, FOUND, PERMISSION_NEEDED, OFF_CAMPUS, UNAVAILABLE }

/** A meeting point with the walk times and map position computed for this request. */
data class RankedPoint(
    val point: MeetingPoint,
    val walkMinutesMe: Int?,
    val walkMinutesOther: Int,
    val map: MapPosition,
)

data class MeetingPointUiState(
    val isLoading: Boolean = true,
    val mode: RankingMode = RankingMode.DAYTIME,
    val time: LocalTime? = null,
    /** Best first; at night only monitored zones are in the list. */
    val ranked: List<RankedPoint> = emptyList(),
    val myLocation: MyLocationStatus = MyLocationStatus.LOCATING,
    val myMapPosition: MapPosition? = null,
    val otherMapPosition: MapPosition? = null,
    val isLive: Boolean = true,
) {
    val recommended: RankedPoint? get() = ranked.firstOrNull()
}

/** Campus Guardian: ranks meeting points from both people's locations and the time of day. */
class MeetingPointViewModel(
    private val productId: String,
    private val meetingPoints: MeetingPointRepository,
    private val myLocation: LocationDataSource,
    private val counterpart: CounterpartLocationSource,
    private val strategies: RankingStrategySelector,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(MeetingPointUiState())
    val state: StateFlow<MeetingPointUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    /** Re-reads the clock, the points and both locations; call again after location permission is granted. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val now = LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
            val strategy = strategies.strategyFor(now)
            val points = meetingPoints.load()
            val other = counterpart.locationOf(productId)

            // Show a suggestion right away from the other person's position; the GPS fix can take seconds.
            publish(points, strategy, now, me = null, other = other, status = MyLocationStatus.LOCATING)

            val (me, status) = locateMe(points)
            publish(points, strategy, now, me, other, status)
        }
    }

    private suspend fun locateMe(points: MeetingPoints): Pair<GeoPoint?, MyLocationStatus> =
        when (val result = myLocation.currentLocation()) {
            is LocationResult.Fix ->
                if (isOnCampus(result.point, points.points)) result.point to MyLocationStatus.FOUND
                else null to MyLocationStatus.OFF_CAMPUS
            LocationResult.PermissionDenied -> null to MyLocationStatus.PERMISSION_NEEDED
            LocationResult.ProviderDisabled, LocationResult.Unavailable -> null to MyLocationStatus.UNAVAILABLE
        }

    // Someone across town would skew every walk time, so their position is ignored until they reach campus.
    private fun isOnCampus(me: GeoPoint, points: List<MeetingPoint>): Boolean =
        points.mapNotNull { it.location }.any { Proximity.distanceMeters(me, it) <= ON_CAMPUS_RADIUS_METERS }

    private fun publish(
        points: MeetingPoints,
        strategy: MeetingPointRankingStrategy,
        now: LocalTime,
        me: GeoPoint?,
        other: GeoPoint,
        status: MyLocationStatus,
    ) {
        val located = points.points.filter { it.location != null }
        val byId = located.associateBy { it.id }
        val ranked = strategy.rank(located.map { ZoneCandidate(it.id, it.isMonitored, it.location!!) }, me, other)
        val projection = CampusMapProjection(located.mapNotNull { it.location } + listOfNotNull(me, other))

        _state.value = MeetingPointUiState(
            isLoading = false,
            mode = strategy.mode,
            time = now,
            ranked = ranked.map {
                RankedPoint(byId.getValue(it.zone.id), it.walkMinutesMe, it.walkMinutesOther, projection.project(it.zone.location))
            },
            myLocation = status,
            myMapPosition = me?.let(projection::project),
            otherMapPosition = projection.project(other),
            isLive = points.isLive,
        )
    }

    companion object {
        const val ON_CAMPUS_RADIUS_METERS = 1_500.0

        fun factory(container: AppContainer, productId: String) = viewModelFactory {
            initializer {
                MeetingPointViewModel(
                    productId = productId,
                    meetingPoints = container.meetingPointRepository,
                    myLocation = container.locationDataSource,
                    counterpart = container.counterpartLocationSource,
                    strategies = container.rankingStrategySelector,
                    clock = container.clock,
                )
            }
        }
    }
}
