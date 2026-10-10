package com.campusswap.app.data.sync

import com.campusswap.app.data.chat.ChatRemoteDataSource
import com.campusswap.app.data.local.OutboxDao
import com.campusswap.app.data.local.PendingListingEntity
import com.campusswap.app.data.local.PendingMessageEntity
import com.campusswap.app.data.materials.CreateMaterialRequest
import com.campusswap.app.data.materials.MaterialDto
import com.campusswap.app.data.materials.MaterialRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OutboxSyncTest {

    private class FakeOutbox : OutboxDao {
        val listings = MutableStateFlow(listOf<PendingListingEntity>())
        val messages = MutableStateFlow(listOf<PendingMessageEntity>())
        override fun observeListings() = listings
        override suspend fun listingsToSend(ownerId: String) = listings.value.filter { it.ownerId == ownerId && it.failedReason == null }
        override suspend fun insertListing(listing: PendingListingEntity): Long {
            val id = listings.value.size + 1L
            listings.value += listing.copy(localId = id)
            return id
        }
        override suspend fun deleteListing(localId: Long) { listings.value = listings.value.filterNot { it.localId == localId } }
        override suspend fun markListingAttempt(localId: Long, reason: String?) {
            listings.value = listings.value.map { if (it.localId == localId) it.copy(attempts = it.attempts + 1, failedReason = reason) else it }
        }
        override fun observeMessages() = messages
        override suspend fun messagesToSend(ownerId: String) = messages.value.filter { it.ownerId == ownerId && it.failedReason == null }
        override suspend fun insertMessage(message: PendingMessageEntity) { messages.value += message }
        override suspend fun deleteMessage(localId: String) { messages.value = messages.value.filterNot { it.localId == localId } }
        override suspend fun markMessageAttempt(localId: String, reason: String?) {
            messages.value = messages.value.map { if (it.localId == localId) it.copy(attempts = it.attempts + 1, failedReason = reason) else it }
        }
    }

    private class FakeMaterials(var outcome: (CreateMaterialRequest) -> Unit = {}) : MaterialRemoteDataSource {
        val created = mutableListOf<CreateMaterialRequest>()
        override suspend fun getMaterials() = emptyList<MaterialDto>()
        override suspend fun create(request: CreateMaterialRequest): MaterialDto {
            outcome(request)
            created += request
            return MaterialDto("srv-${created.size}", request.title, request.description, null, request.price, null, "AVAILABLE", request.category, "u1")
        }
    }

    private class FakeChat(var failOn: String? = null) : ChatRemoteDataSource {
        val rooms = mutableListOf<String>()
        val sent = mutableListOf<String>()
        override suspend fun openRoom(materialId: String): String { rooms += materialId; return "room-$materialId" }
        override suspend fun send(chatRoomId: String, content: String) {
            if (content == failOn) throw IOException("signal lost")
            sent += content
        }
    }

    private fun listing(owner: String = "u1", title: String = "Calculus") = PendingListingEntity(
        ownerId = owner, title = title, description = "Barely used", courseCode = "MATE1203",
        price = "45000", condition = "LIKE_NEW", category = "LAB_SUPPLIES", createdAtMillis = 1,
    )

    private fun message(id: String, content: String, material: String = "m1") = PendingMessageEntity(id, "u1", material, content, 1)

    private fun http(code: Int, body: String = "") = HttpException(Response.error<Any>(code, ResponseBody.create(null, body)))

    @Test fun sentListingsLeaveTheOutboxWithServerEnums() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()) }
        val materials = FakeMaterials()

        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, materials, FakeChat()).flush("u1"))

        assertTrue(outbox.listings.value.isEmpty())
        assertEquals("LAB_EQUIPMENT", materials.created.single().category)
        assertEquals("LIKE_NEW", materials.created.single().condition)
    }

    @Test fun noNetworkKeepsEverythingQueued() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()) }

        val outcome = OutboxSync(outbox, FakeMaterials { throw IOException() }, FakeChat()).flush("u1")

        assertEquals(SyncOutcome.RETRY_LATER, outcome)
        assertEquals(1, outbox.listings.value.single().attempts)
        assertEquals(null, outbox.listings.value.single().failedReason)
    }

    @Test fun refusedListingIsKeptWithTheServerReason() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()); insertListing(listing(title = "Second")) }
        val materials = FakeMaterials { if (it.title == "Calculus") throw http(400, """{"message":["title must be longer than or equal to 2 characters"]}""") }

        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, materials, FakeChat()).flush("u1"))

        assertEquals("title must be longer than or equal to 2 characters", outbox.listings.value.single().failedReason)
        assertEquals(listOf("Second"), materials.created.map { it.title })
    }

    @Test fun expiredSessionAndServerErrorsAreRetried() = runTest {
        for (code in listOf(401, 503)) {
            val outbox = FakeOutbox().apply { insertListing(listing()) }
            assertEquals(SyncOutcome.RETRY_LATER, OutboxSync(outbox, FakeMaterials { throw http(code) }, FakeChat()).flush("u1"))
            assertEquals(1, outbox.listings.value.size)
        }
    }

    @Test fun anotherUsersItemsAreNotSent() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing(owner = "someone-else")) }
        val materials = FakeMaterials()

        OutboxSync(outbox, materials, FakeChat()).flush("u1")

        assertTrue(materials.created.isEmpty())
        assertEquals(1, outbox.listings.value.size)
    }

    @Test fun messagesKeepTheirOrderAndStopAtTheFirstFailure() = runTest {
        val outbox = FakeOutbox().apply {
            insertMessage(message("1", "hi"))
            insertMessage(message("2", "still available?"))
            insertMessage(message("3", "thanks"))
        }
        val chat = FakeChat(failOn = "still available?")

        assertEquals(SyncOutcome.RETRY_LATER, OutboxSync(outbox, FakeMaterials(), chat).flush("u1"))

        assertEquals(listOf("hi"), chat.sent)
        assertEquals(listOf("2", "3"), outbox.messages.value.map { it.localId })
    }

    @Test fun oneRoomIsOpenedPerListing() = runTest {
        val outbox = FakeOutbox().apply {
            insertMessage(message("1", "a", material = "m1"))
            insertMessage(message("2", "b", material = "m1"))
            insertMessage(message("3", "c", material = "m2"))
        }
        val chat = FakeChat()

        OutboxSync(outbox, FakeMaterials(), chat).flush("u1")

        assertEquals(listOf("m1", "m2"), chat.rooms)
        assertEquals(listOf("a", "b", "c"), chat.sent)
        assertTrue(outbox.messages.value.isEmpty())
    }
}
