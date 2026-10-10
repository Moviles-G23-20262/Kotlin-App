package com.campusswap.app.data.sync

import com.campusswap.app.data.Condition
import com.campusswap.app.data.SellDraft
import com.campusswap.app.data.local.OutboxDao
import com.campusswap.app.data.local.PendingListingEntity
import com.campusswap.app.data.local.PendingMessageEntity
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

    fun syncNow() = scheduleSync()
}
