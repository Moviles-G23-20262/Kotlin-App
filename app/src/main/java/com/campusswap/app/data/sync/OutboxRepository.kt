package com.campusswap.app.data.sync

import com.campusswap.app.data.Condition
import com.campusswap.app.data.MeetingPoint
import com.campusswap.app.data.SellDraft
import com.campusswap.app.data.TimeSlot
import com.campusswap.app.data.TransactionRating
import com.campusswap.app.data.local.OutboxDao
import com.campusswap.app.data.local.PendingActionEntity
import com.campusswap.app.data.local.PendingListingEntity
import com.campusswap.app.data.local.PendingMessageEntity
import com.campusswap.app.data.local.cacheGson
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.flow.Flow
import java.time.Clock

/** What the screens use to queue work for the server; [scheduleSync] starts the background send. */
class OutboxRepository(
    private val outbox: OutboxDao,
    private val clock: Clock,
    private val scheduleSync: () -> Unit,
) {
    val pendingListings: Flow<List<PendingListingEntity>> = outbox.observeListings()
    val pendingMessages: Flow<List<PendingMessageEntity>> = outbox.observeMessages()
    val pendingActions: Flow<List<PendingActionEntity>> = outbox.observeActions()

    suspend fun queueListing(draft: SellDraft, ownerId: String): PendingListingEntity {
        val listing = PendingListingEntity(
            ownerId = ownerId,
            title = draft.title.trim(),
            description = draft.description.trim(),
            courseCode = draft.course?.takeUnless { draft.notAssociatedWithCourse }?.code,
            price = draft.price,
            condition = (draft.condition ?: Condition.GOOD).name,
            category = draft.category.name,
            createdAtMillis = clock.millis(),
            photoPaths = draft.photoPaths.joinToString("\n").ifEmpty { null },
        )
        val saved = listing.copy(localId = outbox.insertListing(listing))
        scheduleSync()
        return saved
    }

    suspend fun queueMessage(localId: String, ownerId: String, materialId: String, content: String) {
        outbox.insertMessage(PendingMessageEntity(localId, ownerId, materialId, content, clock.millis()))
        scheduleSync()
    }

    suspend fun discardListing(localId: Long) = outbox.deleteListing(localId)

    suspend fun queueCheckIn(ownerId: String, target: ExchangeTarget, location: GeoPoint?) = queueAction(
        ownerId, PendingActionKind.CHECK_IN, target.productId,
        CheckInPayload(target.buyerId, target.sellerId, target.price, target.meetingPointId, location?.lat, location?.lng),
    )

    suspend fun queueRating(ownerId: String, ratedId: String, rating: TransactionRating) = queueAction(
        ownerId, PendingActionKind.RATING, rating.productId,
        RatingPayload(ratedId, rating.stars, rating.tags.map { it.label }, rating.review.ifBlank { null }, rating.confirmedCondition.name),
    )

    /** False when the slot has no exact times (they come from the server), so it can't be sent later. */
    suspend fun queueProposal(ownerId: String, productId: String, point: MeetingPoint, slot: TimeSlot): Boolean {
        val startsAt = slot.startsAt ?: return false
        val endsAt = slot.endsAt ?: return false
        queueAction(
            ownerId, PendingActionKind.PROPOSAL, productId,
            ProposalPayload(point, slot.id, slot.label, slot.day, slot.isSharedBreak, startsAt.toString(), endsAt.toString()),
        )
        return true
    }

    suspend fun discardAction(localId: Long) = outbox.deleteAction(localId)

    private suspend fun queueAction(ownerId: String, kind: PendingActionKind, productId: String, payload: Any) {
        outbox.insertAction(
            PendingActionEntity(
                ownerId = ownerId,
                kind = kind.name,
                productId = productId,
                payload = cacheGson.toJson(payload),
                createdAtMillis = clock.millis(),
            ),
        )
        scheduleSync()
    }

    fun syncNow() = scheduleSync()
}
