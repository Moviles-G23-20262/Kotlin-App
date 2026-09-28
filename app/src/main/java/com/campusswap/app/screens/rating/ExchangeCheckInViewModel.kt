package com.campusswap.app.screens.rating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.data.ConfirmResult
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.LocationResult
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import com.campusswap.app.domain.Proximity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ManualReason { PERMISSION_DENIED, LOCATION_OFF, NO_FIX, POINT_NOT_MAPPED }

enum class FailReason { OFFLINE, NOT_SYNCED, REJECTED }

sealed interface CheckInState {
    data object Idle : CheckInState
    data object Locating : CheckInState
    data class TooFar(val distanceMeters: Int) : CheckInState
    /** GPS can't be used; the user may still confirm by hand. */
    data class Manual(val reason: ManualReason) : CheckInState
    data object Sending : CheckInState
    /** [verifiedByGps] is null when the confirmation happened earlier in the session. */
    data class Confirmed(val verifiedByGps: Boolean?) : CheckInState
    data class Failed(val reason: FailReason) : CheckInState
}

/** Confirms an exchange by checking the buyer is within [Proximity.CONFIRMATION_RADIUS_METERS] of the meeting point. */
class ExchangeCheckInViewModel(
    private val target: ExchangeTarget,
    private val location: LocationDataSource,
    private val exchanges: ExchangeRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<CheckInState> = _state.asStateFlow()

    // Kept so a retry after a network failure resends the same fix instead of asking for a new one.
    private var lastLocation: GeoPoint? = null

    /** Call only once precise location permission is granted. */
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

    fun confirmManually() = send(location = null)

    fun retry() = send(lastLocation)

    private fun onFix(current: GeoPoint, meetingPoint: GeoPoint) {
        val distance = Proximity.distanceMeters(current, meetingPoint)
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
                ConfirmResult.Success -> CheckInState.Confirmed(verifiedByGps = location != null)
                ConfirmResult.Offline -> CheckInState.Failed(FailReason.OFFLINE)
                ConfirmResult.NotSynced -> CheckInState.Failed(FailReason.NOT_SYNCED)
                is ConfirmResult.Rejected -> CheckInState.Failed(FailReason.REJECTED)
            }
        }
    }

    private fun initialState(): CheckInState = when {
        exchanges.isConfirmed(target.productId) -> CheckInState.Confirmed(verifiedByGps = null)
        target.meetingPoint == null -> CheckInState.Manual(ManualReason.POINT_NOT_MAPPED)
        else -> CheckInState.Idle
    }

    companion object {
        fun factory(container: AppContainer, target: ExchangeTarget) = viewModelFactory {
            initializer { ExchangeCheckInViewModel(target, container.locationDataSource, container.exchangeRepository) }
        }
    }
}
