package com.campusswap.app.screens.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.analytics.Analytics
import com.campusswap.app.analytics.Events
import com.campusswap.app.data.MeetingPoint
import com.campusswap.app.data.MeetingProposal
import com.campusswap.app.data.ProposalResult
import com.campusswap.app.data.MeetingPointPopularityRepository
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.MeetingPoints
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.TimeSlot
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class MyLocationStatus { LOCATING, FOUND, PERMISSION_NEEDED, OFF_CAMPUS, UNAVAILABLE }

data class RankedPoint(
    val point: MeetingPoint,
    val walkMinutesMe: Int?,
    val walkMinutesOther: Int?,
    val map: MapPosition,
    val isPopular: Boolean = false,
)

data class MeetingPointUiState(
    val isLoading: Boolean = true,
    val mode: RankingMode = RankingMode.DAYTIME,
    val time: LocalTime? = null,
    val ranked: List<RankedPoint> = emptyList(),
    val myLocation: MyLocationStatus = MyLocationStatus.LOCATING,
    val myMapPosition: MapPosition? = null,
    val otherMapPosition: MapPosition? = null,
    val knowsCounterpart: Boolean = false,
    val isLive: Boolean = true,
    val slots: List<TimeSlot> = emptyList(),
    val isProposing: Boolean = false,
) {
    val recommended: RankedPoint? get() = ranked.firstOrNull()
}

class MeetingPointViewModel(
    private val productId: String,
    private val meetingPoints: MeetingPointRepository,
    private val myLocation: LocationDataSource,
    private val counterpart: CounterpartLocationSource,
    private val strategies: RankingStrategySelector,
    private val clock: Clock,
    private val popularity: MeetingPointPopularityRepository,
    private val proposals: MeetingProposalRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MeetingPointUiState())
    val state: StateFlow<MeetingPointUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var popularIds: Set<String> = emptySet()
    private var slots: List<TimeSlot> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val now = LocalTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
            val strategy = strategies.strategyFor(now)
            val points = meetingPoints.load()
            val other = counterpart.locationOf(productId)

            launch { markPopular(popularity.popularAt(now.hour)) }
            launch { showSlots(proposals.freeSlots(productId)) }

            publish(points, strategy, now, me = null, other = other, status = MyLocationStatus.LOCATING)

            val (me, status) = locateMe(points)
            publish(points, strategy, now, me, other, status)
        }
    }

    fun propose(point: MeetingPoint, slot: TimeSlot, onResult: (MeetingProposal?, String?) -> Unit) {
        if (_state.value.isProposing) return
        _state.update { it.copy(isProposing = true) }
        viewModelScope.launch {
            val result = proposals.propose(productId, point, slot)
            _state.update { it.copy(isProposing = false) }
            when (result) {
                is ProposalResult.Success -> {
                    Analytics.log(Events.MEETING_PROPOSED, "product_id" to productId)
                    onResult(result.proposal, null)
                }
                ProposalResult.Offline -> onResult(null, "No connection. Your proposal wasn't sent.")
                ProposalResult.NotSynced ->
                    onResult(null, "This listing or meeting point isn't on the server, so the proposal can't be sent.")
                is ProposalResult.Rejected -> onResult(null, result.message ?: "The server couldn't save the proposal.")
            }
        }
    }

    private fun showSlots(freeSlots: List<TimeSlot>) {
        slots = freeSlots
        _state.update { it.copy(slots = freeSlots) }
    }

    private fun markPopular(ids: Set<String>) {
        popularIds = ids
        _state.update { state -> state.copy(ranked = state.ranked.map { it.copy(isPopular = isPopular(it.point)) }) }
    }

    private fun isPopular(point: MeetingPoint): Boolean =
        point.id in popularIds || SeedIds.meetingPoint(point.id) in popularIds

    private suspend fun locateMe(points: MeetingPoints): Pair<GeoPoint?, MyLocationStatus> =
        when (val result = myLocation.currentLocation()) {
            is LocationResult.Fix ->
                if (isOnCampus(result.point, points.points)) result.point to MyLocationStatus.FOUND
                else null to MyLocationStatus.OFF_CAMPUS
            LocationResult.PermissionDenied -> null to MyLocationStatus.PERMISSION_NEEDED
            LocationResult.ProviderDisabled, LocationResult.Unavailable -> null to MyLocationStatus.UNAVAILABLE
        }

    private fun isOnCampus(me: GeoPoint, points: List<MeetingPoint>): Boolean =
        points.mapNotNull { it.location }.any { Proximity.distanceMeters(me, it) <= ON_CAMPUS_RADIUS_METERS }

    private fun publish(
        points: MeetingPoints,
        strategy: MeetingPointRankingStrategy,
        now: LocalTime,
        me: GeoPoint?,
        other: GeoPoint?,
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
                val point = byId.getValue(it.zone.id)
                RankedPoint(point, it.walkMinutesMe, it.walkMinutesOther, projection.project(it.zone.location), isPopular(point))
            },
            myLocation = status,
            myMapPosition = me?.let(projection::project),
            otherMapPosition = other?.let(projection::project),
            knowsCounterpart = other != null,
            isLive = points.isLive,
            slots = slots,
            isProposing = _state.value.isProposing,
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
                    popularity = container.popularityRepository,
                    proposals = container.meetingProposalRepository,
                )
            }
        }
    }
}
