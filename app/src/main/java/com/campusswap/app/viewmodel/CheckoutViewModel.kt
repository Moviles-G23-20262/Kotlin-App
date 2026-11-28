package com.campusswap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.MeetingPoint
import com.campusswap.app.model.MeetingPointRepository
import com.campusswap.app.model.TimeSlot
import com.campusswap.app.model.domain.CampusMeetupTimes
import com.campusswap.app.model.domain.MeetupDay
import com.campusswap.app.model.orders.MeetupPlan
import com.campusswap.app.model.orders.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

data class MeetupChoice(
    val point: MeetingPoint? = null,
    val dayIndex: Int = 0,
    val slot: TimeSlot? = null,
    val payment: PaymentMethod? = null,
) {
    val isComplete: Boolean get() = missing.isEmpty()

    val missing: List<String>
        get() = listOfNotNull(
            "place".takeIf { point == null },
            "time".takeIf { slot == null },
            "payment".takeIf { payment == null },
        )
}

data class CheckoutUiState(
    val points: List<MeetingPoint> = emptyList(),
    val days: List<MeetupDay> = emptyList(),
    val choices: Map<String, MeetupChoice> = emptyMap(),
) {
    fun choiceFor(productId: String) = choices[productId] ?: MeetupChoice()

    fun isComplete(productIds: Collection<String>) = productIds.all { choiceFor(it).isComplete }

    fun incompleteCount(productIds: Collection<String>) = productIds.count { !choiceFor(it).isComplete }

    fun takenByOthers(productId: String): Set<String> =
        choices.filterKeys { it != productId }.values.mapNotNull { it.slot?.id }.toSet()

    fun plans(): Map<String, MeetupPlan> = choices.mapNotNull { (productId, choice) ->
        val point = choice.point ?: return@mapNotNull null
        val slot = choice.slot ?: return@mapNotNull null
        val payment = choice.payment ?: return@mapNotNull null
        productId to MeetupPlan(point, slot, payment)
    }.toMap()
}

class CheckoutViewModel(
    private val meetingPoints: MeetingPointRepository,
    clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutUiState(days = CampusMeetupTimes.upcoming(clock)))
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val points = meetingPoints.load().points.sortedWith(compareByDescending<MeetingPoint> { it.isMonitored }.thenBy { it.name })
            _state.update { it.copy(points = points) }
        }
    }

    fun choosePoint(productId: String, point: MeetingPoint) = change(productId) { it.copy(point = point) }

    fun chooseDay(productId: String, dayIndex: Int) = change(productId) {
        if (it.dayIndex == dayIndex) it else it.copy(dayIndex = dayIndex, slot = null)
    }

    fun chooseSlot(productId: String, slot: TimeSlot) {
        if (slot.id in _state.value.takenByOthers(productId)) return
        change(productId) { it.copy(slot = slot) }
    }

    fun choosePayment(productId: String, method: PaymentMethod) = change(productId) { it.copy(payment = method) }

    private fun change(productId: String, edit: (MeetupChoice) -> MeetupChoice) = _state.update {
        it.copy(choices = it.choices + (productId to edit(it.choiceFor(productId))))
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { CheckoutViewModel(container.meetingPointRepository, container.clock) }
        }
    }
}
