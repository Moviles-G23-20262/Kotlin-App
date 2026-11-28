package com.campusswap.app.model.orders

import com.campusswap.app.model.ChatRepository
import com.campusswap.app.model.ChatRoomRepository
import com.campusswap.app.model.MeetingPoint
import com.campusswap.app.model.MeetingProposalRepository
import com.campusswap.app.model.Order
import com.campusswap.app.model.TimeSlot
import kotlinx.coroutines.CancellationException

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    NEQUI("Nequi"),
    DAVIPLATA("Daviplata"),
}

data class MeetupPlan(val point: MeetingPoint, val slot: TimeSlot, val payment: PaymentMethod)

class MeetupArranger(
    private val rooms: ChatRoomRepository,
    private val proposals: MeetingProposalRepository,
    private val chat: ChatRepository,
) {
    suspend fun arrange(order: Order, plans: Map<String, MeetupPlan>) {
        order.products.forEach { product ->
            try {
                val room = rooms.roomFor(product.id) ?: return@forEach
                val plan = plans[product.id]
                plan?.let { proposals.propose(product.id, it.point, it.slot, room) }
                val paying = plan?.let { " I'll pay with ${it.payment.label} when we meet." }.orEmpty()
                chat.send(product.id, "Hi! I just ordered ${product.title}.$paying")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
    }
}
