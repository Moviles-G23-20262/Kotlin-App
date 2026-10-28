package com.campusswap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.analytics.Events
import com.campusswap.app.model.ConfirmResult
import com.campusswap.app.model.ExchangeRepository
import com.campusswap.app.model.local.PendingActionEntity
import com.campusswap.app.model.location.LocationDataSource
import com.campusswap.app.model.location.LocationResult
import com.campusswap.app.model.sync.CheckInPayload
import com.campusswap.app.model.sync.PendingActionKind
import com.campusswap.app.model.sync.payloadAs
import com.campusswap.app.model.domain.ExchangeTarget
import com.campusswap.app.model.domain.GeoPoint
import com.campusswap.app.model.domain.Proximity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ManualReason { PERMISSION_DENIED, LOCATION_OFF, NO_FIX, POINT_NOT_MAPPED }

enum class FailReason { OFFLINE, NOT_SYNCED, REJECTED }

sealed interface CheckInState {
    data object Idle : CheckInState
    data object Locating : CheckInState
    data class TooFar(val distanceMeters: Int) : CheckInState
    data class Manual(val reason: ManualReason) : CheckInState
    data object Sending : CheckInState
    data class Confirmed(val verifiedByGps: Boolean?) : CheckInState
    /** No connection: saved on the phone and sent by the outbox when the signal comes back. */
    data class Queued(val verifiedByGps: Boolean) : CheckInState
    data class Failed(val reason: FailReason, val detail: String? = null) : CheckInState
}

/** The outbox, seen from the check-in screen. */
interface CheckInQueue {
    /** This exchange's check-in while it waits in the outbox; null once it's sent (or if there's none). */
    val pending: Flow<PendingActionEntity?>
    suspend fun queue(location: GeoPoint?): Boolean
    suspend fun discard(localId: Long)

    object None : CheckInQueue {
        override val pending: Flow<PendingActionEntity?> = flowOf(null)
        override suspend fun queue(location: GeoPoint?) = false
        override suspend fun discard(localId: Long) = Unit
    }
}

class ExchangeCheckInViewModel(
    private val target: ExchangeTarget,
    private val location: LocationDataSource,
    private val exchanges: ExchangeRepository,
    private val queue: CheckInQueue = CheckInQueue.None,
    private val logEvent: (name: String, properties: Map<String, Any?>) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<CheckInState> = _state.asStateFlow()

    private var lastLocation: GeoPoint? = null
    private var lastDistanceMeters: Int? = null
    private var manualReason: ManualReason? = null

    init {
        viewModelScope.launch {
            queue.pending.collect { row ->
                val current = _state.value
                when {
                    // Left the outbox without failing: the background sync delivered it.
                    row == null -> if (current is CheckInState.Queued) _state.value = CheckInState.Confirmed(current.verifiedByGps)
                    row.failedReason != null -> {
                        // Shown once; trying again starts a fresh check-in.
                        queue.discard(row.localId)
                        _state.value = CheckInState.Failed(FailReason.REJECTED, row.failedReason)
                    }
                    else -> _state.value = CheckInState.Queued(verifiedByGps = row.payloadAs<CheckInPayload>()?.lat != null)
                }
            }
        }
    }

    fun checkIn() {
        val point = target.meetingPoint ?: return
        if (_state.value == CheckInState.Locating || _state.value == CheckInState.Sending) return
        viewModelScope.launch {
            _state.value = CheckInState.Locating
            when (val result = location.currentLocation()) {
                is LocationResult.Fix -> onFix(result.point, point)
                LocationResult.PermissionDenied -> onPermissionDenied()
                LocationResult.ProviderDisabled -> _state.value = CheckInState.Manual(ManualReason.LOCATION_OFF)
                LocationResult.Unavailable -> _state.value = CheckInState.Manual(ManualReason.NO_FIX)
            }
        }
    }

    fun onPermissionDenied() {
        _state.value = CheckInState.Manual(ManualReason.PERMISSION_DENIED)
    }

    fun confirmManually() {
        manualReason = (_state.value as? CheckInState.Manual)?.reason ?: manualReason
        send(location = null)
    }

    fun retry() = send(lastLocation)

    private fun onFix(current: GeoPoint, meetingPoint: GeoPoint) {
        val distance = Proximity.distanceMeters(current, meetingPoint)
        lastDistanceMeters = distance.roundToInt()
        if (distance <= Proximity.CONFIRMATION_RADIUS_METERS) {
            send(current)
        } else {
            _state.value = CheckInState.TooFar(distance.roundToInt())
        }
    }

    private fun send(location: GeoPoint?) {
        lastLocation = location
        viewModelScope.launch {
            _state.value = CheckInState.Sending
            _state.value = when (exchanges.confirm(target, location)) {
                ConfirmResult.Success -> {
                    logConfirmed(location)
                    CheckInState.Confirmed(verifiedByGps = location != null)
                }
                ConfirmResult.Offline ->
                    if (queue.queue(location)) {
                        logConfirmed(location, queued = true)
                        CheckInState.Queued(verifiedByGps = location != null)
                    } else {
                        CheckInState.Failed(FailReason.OFFLINE)
                    }
                ConfirmResult.NotSynced -> CheckInState.Failed(FailReason.NOT_SYNCED)
                is ConfirmResult.Rejected -> CheckInState.Failed(FailReason.REJECTED)
            }
        }
    }

    private fun logConfirmed(location: GeoPoint?, queued: Boolean = false) {
        val byGps = location != null
        logEvent(
            Events.EXCHANGE_CONFIRMED,
            mapOf(
                "product_id" to target.productId,
                "meeting_point_id" to target.meetingPointId,
                "method" to if (byGps) "gps" else "manual",
                "lat" to location?.lat,
                "lng" to location?.lng,
                "distance_m" to if (byGps) lastDistanceMeters else null,
                "manual_reason" to if (byGps) null else manualReason?.name?.lowercase(),
                "queued_offline" to queued,
            ),
        )
    }

    private fun initialState(): CheckInState = when {
        exchanges.isConfirmed(target.productId) -> CheckInState.Confirmed(verifiedByGps = null)
        target.meetingPoint == null -> CheckInState.Manual(ManualReason.POINT_NOT_MAPPED)
        else -> CheckInState.Idle
    }

    companion object {
        fun factory(container: AppContainer, target: ExchangeTarget) = viewModelFactory {
            initializer {
                ExchangeCheckInViewModel(
                    target,
                    container.locationDataSource,
                    container.exchangeRepository,
                    OutboxCheckInQueue(container, target),
                ) { name, properties ->
                    Analytics.log(name, *properties.toList().toTypedArray())
                }
            }
        }
    }
}

private class OutboxCheckInQueue(private val container: AppContainer, private val target: ExchangeTarget) : CheckInQueue {
    private val userId get() = container.sessionManager.validSession()?.userId

    override val pending: Flow<PendingActionEntity?> = container.outboxRepository.pendingActions.map { rows ->
        rows.lastOrNull {
            it.kind == PendingActionKind.CHECK_IN.name && it.productId == target.productId && it.ownerId == userId
        }
    }

    override suspend fun queue(location: GeoPoint?): Boolean {
        val owner = userId ?: return false
        container.outboxRepository.queueCheckIn(owner, target, location)
        return true
    }

    override suspend fun discard(localId: Long) = container.outboxRepository.discardAction(localId)
}
