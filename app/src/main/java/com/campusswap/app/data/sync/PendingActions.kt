package com.campusswap.app.data.sync

import com.campusswap.app.data.ConfirmResult
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.MeetingPoint
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.ProposalResult
import com.campusswap.app.data.TimeSlot
import com.campusswap.app.data.local.PendingActionEntity
import com.campusswap.app.data.local.cacheGson
import com.campusswap.app.data.ratings.RatingRepository
import com.campusswap.app.data.ratings.RatingResult
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import java.time.Clock
import java.time.Instant

/** What the user can do offline besides publishing and chatting. */
enum class PendingActionKind { CHECK_IN, RATING, PROPOSAL }

/** "I'm at the meeting point and got the item": completes the exchange on the server. */
data class CheckInPayload(
    val buyerId: String,
    val sellerId: String,
    val price: Double,
    val meetingPointId: String?,
    val lat: Double?,
    val lng: Double?,
) {
    fun target(productId: String) = ExchangeTarget(productId, buyerId, sellerId, price, meetingPointId, meetingPoint = null)
    fun location(): GeoPoint? = if (lat != null && lng != null) GeoPoint(lat, lng) else null
}

data class RatingPayload(
    val ratedId: String,
    val stars: Int,
    val tags: List<String>,
    val review: String?,
    /** [com.campusswap.app.data.Condition] name the buyer confirmed, to show the rating again after a restart. */
    val condition: String,
)

/** Times travel as ISO text: Gson can't rebuild [Instant] on its own. */
data class ProposalPayload(
    val point: MeetingPoint,
    val slotId: String,
    val slotLabel: String,
    val slotDay: String,
    val sharedBreak: Boolean,
    val startsAt: String,
    val endsAt: String,
) {
    fun slot() = TimeSlot(slotId, slotLabel, slotDay, sharedBreak, Instant.parse(startsAt), Instant.parse(endsAt))
}

inline fun <reified T> PendingActionEntity.payloadAs(): T? =
    runCatching { cacheGson.fromJson(payload, T::class.java) }.getOrNull()

sealed interface ActionOutcome {
    data object Sent : ActionOutcome
    data object Retry : ActionOutcome
    data class Refused(val reason: String) : ActionOutcome
}

fun interface PendingActionSender {
    suspend fun send(action: PendingActionEntity): ActionOutcome

    companion object {
        /** Never drops anything: without a real sender the actions just wait. */
        val None = PendingActionSender { ActionOutcome.Retry }
    }
}

/**
 * Sends a queued action through the same repositories the screens use, and turns their result
 * into the outbox's three endings: no network or server trouble waits, a real "no" is kept with
 * its reason so the screen can say why.
 */
class RepositoryActionSender(
    private val exchanges: ExchangeRepository,
    private val ratings: RatingRepository,
    private val proposals: MeetingProposalRepository,
    private val clock: Clock,
) : PendingActionSender {

    override suspend fun send(action: PendingActionEntity): ActionOutcome {
        val kind = PendingActionKind.entries.firstOrNull { it.name == action.kind }
        val outcome = when (kind) {
            PendingActionKind.CHECK_IN -> action.payloadAs<CheckInPayload>()?.let { checkIn(action.productId, it) }
            PendingActionKind.RATING -> action.payloadAs<RatingPayload>()?.let { rate(action.productId, it) }
            PendingActionKind.PROPOSAL -> action.payloadAs<ProposalPayload>()?.let { propose(action.productId, it) }
            null -> null
        }
        return outcome ?: ActionOutcome.Refused("This saved action couldn't be read.")
    }

    private suspend fun checkIn(productId: String, payload: CheckInPayload): ActionOutcome =
        when (val result = exchanges.confirm(payload.target(productId), payload.location())) {
            ConfirmResult.Success -> ActionOutcome.Sent
            ConfirmResult.Offline -> ActionOutcome.Retry
            ConfirmResult.NotSynced -> ActionOutcome.Refused("This listing isn't on the server, so the exchange can't be recorded.")
            is ConfirmResult.Rejected ->
                if (isRetryable(result.httpCode)) ActionOutcome.Retry
                else ActionOutcome.Refused("The server couldn't record this exchange (${result.httpCode}).")
        }

    private suspend fun rate(productId: String, payload: RatingPayload): ActionOutcome =
        when (val result = ratings.submit(productId, payload.ratedId, payload.stars, payload.tags, payload.review)) {
            RatingResult.Saved -> ActionOutcome.Sent
            RatingResult.Offline -> ActionOutcome.Retry
            RatingResult.ExchangeNotRecorded -> ActionOutcome.Refused("This exchange isn't completed on the server, so the rating wasn't saved.")
            is RatingResult.Rejected -> when {
                result.httpCode == 409 -> ActionOutcome.Sent // ya estaba calificado
                isRetryable(result.httpCode) -> ActionOutcome.Retry
                else -> ActionOutcome.Refused("The server didn't accept the rating (${result.httpCode}).")
            }
            RatingResult.Queued, is RatingResult.Refused -> ActionOutcome.Retry
        }

    private suspend fun propose(productId: String, payload: ProposalPayload): ActionOutcome {
        val slot = payload.slot()
        if (slot.startsAt?.isBefore(clock.instant()) == true) {
            return ActionOutcome.Refused("That time already passed before you got back online. Propose a new one.")
        }
        return when (val result = proposals.propose(productId, payload.point, slot)) {
            is ProposalResult.Success -> ActionOutcome.Sent
            ProposalResult.Offline -> ActionOutcome.Retry
            ProposalResult.NotSynced -> ActionOutcome.Refused("This listing or meeting point isn't on the server.")
            is ProposalResult.Rejected ->
                if (result.httpCode?.let(::isRetryable) == true) ActionOutcome.Retry
                else ActionOutcome.Refused(result.message ?: "The server couldn't save the proposal.")
        }
    }
}
