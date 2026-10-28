package com.campusswap.app.model.sync

import com.campusswap.app.model.ChatRoomRepository
import com.campusswap.app.model.FakePhotoUploader
import com.campusswap.app.model.FakeChatRoomRemoteDataSource
import com.campusswap.app.model.remote.MessageDto
import com.campusswap.app.model.remote.MessageRemoteDataSource
import com.campusswap.app.model.local.OutboxDao
import com.campusswap.app.model.local.PendingActionEntity
import com.campusswap.app.model.local.PendingListingEntity
import com.campusswap.app.model.local.PendingMessageEntity
import com.campusswap.app.model.materials.CreateMaterialRequest
import com.campusswap.app.model.materials.MaterialDto
import com.campusswap.app.model.materials.MaterialRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
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
        val actions = MutableStateFlow(listOf<PendingActionEntity>())
        override fun observeActions() = actions
        override suspend fun actionsToSend(ownerId: String) = actions.value.filter { it.ownerId == ownerId && it.failedReason == null }
        override suspend fun insertAction(action: PendingActionEntity): Long {
            val id = actions.value.size + 1L
            actions.value += action.copy(localId = id)
            return id
        }
        override suspend fun deleteAction(localId: Long) { actions.value = actions.value.filterNot { it.localId == localId } }
        override suspend fun markActionAttempt(localId: Long, reason: String?) {
            actions.value = actions.value.map { if (it.localId == localId) it.copy(attempts = it.attempts + 1, failedReason = reason) else it }
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

    private class FakeChat(var failOn: String? = null) : MessageRemoteDataSource {
        val sent = mutableListOf<String>()
        val sentTo = mutableListOf<String>()
        override suspend fun messages(chatRoomId: String): List<MessageDto> = emptyList()
        override suspend fun send(chatRoomId: String, content: String): MessageDto {
            if (content == failOn) throw IOException("signal lost")
            sent += content
            sentTo += chatRoomId
            return MessageDto("m-${sent.size}", chatRoomId, "u1", content, "TEXT", false, "2026-10-05T17:00:00Z", null, null)
        }
    }

    private fun chatRooms() = ChatRoomRepository(FakeChatRoomRemoteDataSource())

    private fun listing(owner: String = "u1", title: String = "Calculus") = PendingListingEntity(
        ownerId = owner, title = title, description = "Barely used", courseCode = "MATE1203",
        price = "45000", condition = "LIKE_NEW", category = "LAB_SUPPLIES", createdAtMillis = 1,
    )

    private val materialA = "b0000000-0000-4000-8000-0000000000a1"
    private val materialB = "b0000000-0000-4000-8000-0000000000b2"

    private fun message(id: String, content: String, material: String = materialA) =
        PendingMessageEntity(id, "u1", material, content, 1)

    private fun http(code: Int, body: String = "") = HttpException(Response.error<Any>(code, ResponseBody.create(null, body)))

    @Test fun sentListingsLeaveTheOutboxWithServerEnums() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()) }
        val materials = FakeMaterials()

        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, materials, chatRooms(), FakeChat()).flush("u1"))

        assertTrue(outbox.listings.value.isEmpty())
        assertEquals("LAB_EQUIPMENT", materials.created.single().category)
        assertEquals("LIKE_NEW", materials.created.single().condition)
    }

    @Test fun noNetworkKeepsEverythingQueued() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()) }

        val outcome = OutboxSync(outbox, FakeMaterials { throw IOException() }, chatRooms(), FakeChat()).flush("u1")

        assertEquals(SyncOutcome.RETRY_LATER, outcome)
        assertEquals(1, outbox.listings.value.single().attempts)
        assertEquals(null, outbox.listings.value.single().failedReason)
    }

    @Test fun refusedListingIsKeptWithTheServerReason() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing()); insertListing(listing(title = "Second")) }
        val materials = FakeMaterials { if (it.title == "Calculus") throw http(400, """{"message":["title must be longer than or equal to 2 characters"]}""") }

        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, materials, chatRooms(), FakeChat()).flush("u1"))

        assertEquals("title must be longer than or equal to 2 characters", outbox.listings.value.single().failedReason)
        assertEquals(listOf("Second"), materials.created.map { it.title })
    }

    @Test fun expiredSessionAndServerErrorsAreRetried() = runTest {
        for (code in listOf(401, 503)) {
            val outbox = FakeOutbox().apply { insertListing(listing()) }
            assertEquals(SyncOutcome.RETRY_LATER, OutboxSync(outbox, FakeMaterials { throw http(code) }, chatRooms(), FakeChat()).flush("u1"))
            assertEquals(1, outbox.listings.value.size)
        }
    }

    @Test fun anotherUsersItemsAreNotSent() = runTest {
        val outbox = FakeOutbox().apply { insertListing(listing(owner = "someone-else")) }
        val materials = FakeMaterials()

        OutboxSync(outbox, materials, chatRooms(), FakeChat()).flush("u1")

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

        assertEquals(SyncOutcome.RETRY_LATER, OutboxSync(outbox, FakeMaterials(), chatRooms(), chat).flush("u1"))

        assertEquals(listOf("hi"), chat.sent)
        assertEquals(listOf("2", "3"), outbox.messages.value.map { it.localId })
    }

    @Test fun oneRoomIsOpenedPerListing() = runTest {
        val outbox = FakeOutbox().apply {
            insertMessage(message("1", "a", material = materialA))
            insertMessage(message("2", "b", material = materialA))
            insertMessage(message("3", "c", material = materialB))
        }
        val chat = FakeChat()
        val roomRemote = FakeChatRoomRemoteDataSource()

        OutboxSync(outbox, FakeMaterials(), ChatRoomRepository(roomRemote), chat).flush("u1")

        assertEquals(listOf(materialA, materialB), roomRemote.openedMaterials)
        assertEquals(listOf("a", "b", "c"), chat.sent)
        assertTrue(outbox.messages.value.isEmpty())
    }

    @Test fun aPhotoSentOfflineIsUploadedThenSentAsItsLink() = runTest {
        val file = File.createTempFile("photo", ".jpg")
        val outbox = FakeOutbox().apply { insertMessage(message("1", "file://${file.path}")) }
        val chat = FakeChat()
        val photos = FakePhotoUploader()

        OutboxSync(outbox, FakeMaterials(), chatRooms(), chat, photos).flush("u1")

        assertEquals(listOf("https://blob.campusswap.test/photo-1.jpg"), chat.sent)
        assertEquals(listOf(file.path), photos.discarded)
        assertTrue(outbox.messages.value.isEmpty())
    }

    @Test fun aPhotoThatLeftThePhoneIsRefusedWithAReason() = runTest {
        val outbox = FakeOutbox().apply { insertMessage(message("1", "file:///gone/photo.jpg")) }
        val chat = FakeChat()

        OutboxSync(outbox, FakeMaterials(), chatRooms(), chat, FakePhotoUploader()).flush("u1")

        assertTrue(chat.sent.isEmpty())
        assertEquals("The photo is no longer on this phone.", outbox.messages.value.single().failedReason)
    }

    @Test fun aMessageGoesToTheRoomItWasWrittenIn() = runTest {
        val outbox = FakeOutbox().apply {
            insertMessage(PendingMessageEntity("1", "u1", materialA, "to luis", 1, roomId = "room-luis"))
        }
        val chat = FakeChat()
        val roomRemote = FakeChatRoomRemoteDataSource()

        OutboxSync(outbox, FakeMaterials(), ChatRoomRepository(roomRemote), chat).flush("u1")

        assertEquals(listOf("room-luis"), chat.sentTo)
        assertEquals(0, roomRemote.openCalls)
    }

    private fun action(kind: PendingActionKind, product: String = materialA) = PendingActionEntity(
        ownerId = "u1", kind = kind.name, productId = product, payload = "{}", createdAtMillis = 1,
    )

    @Test fun actionsGoInOrderAndARatingWaitsForItsCheckIn() = runTest {
        val outbox = FakeOutbox()
        outbox.insertAction(action(PendingActionKind.CHECK_IN))
        outbox.insertAction(action(PendingActionKind.RATING))
        val tried = mutableListOf<String>()
        var online = false
        val sender = PendingActionSender { tried += it.kind; if (online) ActionOutcome.Sent else ActionOutcome.Retry }

        val offline = OutboxSync(outbox, FakeMaterials(), chatRooms(), FakeChat(), actions = sender).flush("u1")

        assertEquals(SyncOutcome.RETRY_LATER, offline)
        assertEquals(listOf("CHECK_IN"), tried)
        assertEquals(2, outbox.actions.value.size)

        online = true
        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, FakeMaterials(), chatRooms(), FakeChat(), actions = sender).flush("u1"))
        assertEquals(listOf("CHECK_IN", "CHECK_IN", "RATING"), tried)
        assertTrue(outbox.actions.value.isEmpty())
    }

    @Test fun aRefusedActionKeepsItsReasonAndTheRestStillGoOut() = runTest {
        val outbox = FakeOutbox()
        outbox.insertAction(action(PendingActionKind.PROPOSAL))
        outbox.insertAction(action(PendingActionKind.RATING))
        val sender = PendingActionSender {
            if (it.kind == "PROPOSAL") ActionOutcome.Refused("That time already passed") else ActionOutcome.Sent
        }

        assertEquals(SyncOutcome.DONE, OutboxSync(outbox, FakeMaterials(), chatRooms(), FakeChat(), actions = sender).flush("u1"))

        val left = outbox.actions.value.single()
        assertEquals("PROPOSAL", left.kind)
        assertEquals("That time already passed", left.failedReason)
    }
}
