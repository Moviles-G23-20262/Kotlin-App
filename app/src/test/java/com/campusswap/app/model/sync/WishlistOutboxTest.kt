package com.campusswap.app.model.sync

import com.campusswap.app.model.ChatRoomRepository
import com.campusswap.app.model.ExchangeRepository
import com.campusswap.app.model.FakeChatRoomRemoteDataSource
import com.campusswap.app.model.FakeExchangeRemoteDataSource
import com.campusswap.app.model.FakeMeetingProposalRemoteDataSource
import com.campusswap.app.model.FakeMessageRemoteDataSource
import com.campusswap.app.model.MeetingProposalRepository
import com.campusswap.app.model.local.OutboxDao
import com.campusswap.app.model.local.PendingActionEntity
import com.campusswap.app.model.local.PendingListingEntity
import com.campusswap.app.model.local.PendingMessageEntity
import com.campusswap.app.model.materials.CreateMaterialRequest
import com.campusswap.app.model.materials.MaterialDto
import com.campusswap.app.model.materials.MaterialRemoteDataSource
import com.campusswap.app.model.ratings.CreateRatingRequest
import com.campusswap.app.model.ratings.RatingRemoteDataSource
import com.campusswap.app.model.ratings.RatingRepository
import com.campusswap.app.model.wishlist.WishlistItemDto
import com.campusswap.app.model.wishlist.WishlistRemoteDataSource
import com.campusswap.app.model.wishlist.WishlistRepository
import com.campusswap.app.model.wishlist.WishlistResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Hearts tapped without a connection reach the server later, in order, or say why they didn't. */
class WishlistOutboxTest {
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val listing = "d1000000-0000-4000-8000-000000000007"
    private val clock = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneOffset.UTC)

    private class FakeOutbox : OutboxDao {
        val actions = MutableStateFlow(listOf<PendingActionEntity>())
        override fun observeListings() = emptyFlow<List<PendingListingEntity>>()
        override suspend fun listingsToSend(ownerId: String) = emptyList<PendingListingEntity>()
        override suspend fun insertListing(listing: PendingListingEntity) = error("not used")
        override suspend fun deleteListing(localId: Long) = Unit
        override suspend fun markListingAttempt(localId: Long, reason: String?) = Unit
        override fun observeMessages() = emptyFlow<List<PendingMessageEntity>>()
        override suspend fun messagesToSend(ownerId: String) = emptyList<PendingMessageEntity>()
        override suspend fun insertMessage(message: PendingMessageEntity) = Unit
        override suspend fun deleteMessage(localId: String) = Unit
        override suspend fun markMessageAttempt(localId: String, reason: String?) = Unit
        override fun observeActions() = actions
        override suspend fun actionsToSend(ownerId: String) = actions.value.filter { it.ownerId == ownerId && it.failedReason == null }
        override suspend fun insertAction(action: PendingActionEntity): Long {
            val id = (actions.value.maxOfOrNull { it.localId } ?: 0L) + 1
            actions.value += action.copy(localId = id)
            return id
        }
        override suspend fun deleteAction(localId: Long) { actions.value = actions.value.filterNot { it.localId == localId } }
        override suspend fun markActionAttempt(localId: Long, reason: String?) {
            actions.value = actions.value.map { if (it.localId == localId) it.copy(attempts = it.attempts + 1, failedReason = reason) else it }
        }
    }

    private class FakeWishlist : WishlistRemoteDataSource {
        var failure: Exception? = null
        val items = mutableListOf<WishlistItemDto>()
        val removed = mutableListOf<String>()
        override suspend fun getAll(): List<WishlistItemDto> = failure?.let { throw it } ?: items.toList()
        override suspend fun add(materialId: String): WishlistItemDto {
            failure?.let { throw it }
            return WishlistItemDto("w${items.size + 1}", "me", materialId, null, null).also { items += it }
        }
        override suspend fun remove(itemId: String) {
            failure?.let { throw it }
            removed += itemId
            items.removeAll { it.id == itemId }
        }
    }

    private object NoMaterials : MaterialRemoteDataSource {
        override suspend fun getMaterials() = emptyList<MaterialDto>()
        override suspend fun create(request: CreateMaterialRequest) = error("not used")
    }

    private object NoRatings : RatingRemoteDataSource {
        override suspend fun getExchanges(materialId: String) = error("not used")
        override suspend fun createRating(request: CreateRatingRequest) = error("not used")
        override suspend fun getRatings(exchangeId: String?, userId: String?) = error("not used")
    }

    private val outbox = FakeOutbox()
    private val remote = FakeWishlist()
    private val wishlist = WishlistRepository(remote, { me })
    private val queue = OutboxRepository(outbox, clock) {}

    private fun sync(): OutboxSync {
        val rooms = ChatRoomRepository(FakeChatRoomRemoteDataSource())
        val sender = RepositoryActionSender(
            ExchangeRepository(FakeExchangeRemoteDataSource()),
            RatingRepository(NoRatings),
            MeetingProposalRepository(rooms, FakeMeetingProposalRemoteDataSource()),
            wishlist,
            clock,
        )
        return OutboxSync(outbox, NoMaterials, rooms, FakeMessageRemoteDataSource(), actions = sender)
    }

    private fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))

    @Test fun noSignalOrServerDownMeansTryLaterButABadRequestIsARealNo() = runTest {
        remote.failure = IOException("airplane mode")
        assertEquals(WishlistResult.OFFLINE, wishlist.add(listing))
        remote.failure = http(503)
        assertEquals(WishlistResult.OFFLINE, wishlist.add(listing))
        remote.failure = http(400)
        assertEquals(WishlistResult.REFUSED, wishlist.add(listing))
        remote.failure = http(409)
        assertEquals(WishlistResult.SAVED, wishlist.add(listing))
        assertEquals(WishlistResult.NOT_ON_SERVER, wishlist.add("pending-1"))
        remote.failure = http(404)
        assertEquals(WishlistResult.SAVED, wishlist.remove(listing))
    }

    @Test fun aListingSavedOfflineReachesTheServerWhenTheSignalComesBack() = runTest {
        queue.queueWishlist(me, listing, added = true)

        remote.failure = IOException("still offline")
        assertEquals(SyncOutcome.RETRY_LATER, sync().flush(me))
        assertEquals(1, outbox.actions.value.size)

        remote.failure = null
        assertEquals(SyncOutcome.DONE, sync().flush(me))
        assertEquals(listOf(listing), remote.items.map { it.materialId })
        assertTrue(outbox.actions.value.isEmpty())
    }

    @Test fun savingAndRemovingTheSameListingOfflineSendsNothing() = runTest {
        queue.queueWishlist(me, listing, added = true)
        queue.queueWishlist(me, listing, added = false)

        assertTrue(outbox.actions.value.isEmpty())
    }

    @Test fun aRemovalQueuedBeforeARestartFindsTheServerItemOnItsOwn() = runTest {
        remote.items += WishlistItemDto("w9", me, listing, null, null)
        queue.queueWishlist(me, listing, added = false)

        // A fresh repository, as when WorkManager runs with the app closed: it doesn't know "w9" yet.
        assertEquals(SyncOutcome.DONE, sync().flush(me))
        assertEquals(listOf("w9"), remote.removed)
    }

    @Test fun aChangeTheServerRefusesStaysWithItsReason() = runTest {
        queue.queueWishlist(me, listing, added = true)
        remote.failure = http(400)

        assertEquals(SyncOutcome.DONE, sync().flush(me))
        val left = outbox.actions.value.single()
        assertEquals(PendingActionKind.WISHLIST_ADD.name, left.kind)
        assertEquals("The server couldn't update your wishlist.", left.failedReason)
    }
}
