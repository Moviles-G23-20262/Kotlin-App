package com.campusswap.app.data

import com.campusswap.app.data.FakeMessageRemoteDataSource.Companion.ME
import com.campusswap.app.data.FakeMessageRemoteDataSource.Companion.OTHER
import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.data.remote.MessageDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ChatRepositoryTest {
    private val rooms = FakeChatRoomRemoteDataSource()
    private val messages = FakeMessageRemoteDataSource()
    private val uploads = FakeUploadRemoteDataSource()
    private val repository = ChatRepository(ChatRoomRepository(rooms), messages, uploads, currentUserId = { ME })

    private val seededProduct = "p1"
    private val localOnlyProduct = "local-7"

    private fun message(id: String, sender: String, text: String, proposal: MeetingProposalDto? = null) = MessageDto(
        id = id,
        chatRoomId = "room-1",
        senderId = sender,
        content = text,
        type = if (proposal == null) "TEXT" else "MEETING",
        isRead = true,
        createdAt = "2026-10-05T17:00:00Z",
        sender = null,
        meetingProposal = proposal,
    )

    @Test
    fun `a listing that only exists in the app has no conversation`() = runTest {
        assertEquals(ChatLoad.NotSynced, repository.messages(localOnlyProduct))
        assertTrue(rooms.openedMaterials.isEmpty())
    }

    @Test
    fun `messages are attributed by sender, not by position`() = runTest {
        messages.stored += message("m1", OTHER, "Is it still available?")
        messages.stored += message("m2", ME, "Yes, it is")

        val loaded = repository.messages(seededProduct) as ChatLoad.Success

        assertEquals(listOf(MessageAuthor.OTHER, MessageAuthor.ME), loaded.messages.map { it.author })
        assertEquals(listOf("12:00", "12:00"), loaded.messages.map { it.time })
    }

    @Test
    fun `a meeting message becomes the proposal card`() = runTest {
        val point = MeetingPointDto("c0000000-0000-4000-8000-000000000001", "Library", null, "LIBRARY", true, 4.6, -74.06)
        val proposal = MeetingProposalDto("prop-1", OTHER, "2026-10-05T17:00:00Z", "2026-10-05T18:00:00Z", "PENDING", null, point)
        messages.stored += message("m1", ME, "Let's meet")
        messages.stored += message("m2", OTHER, "Meeting point proposed", proposal)

        val loaded = repository.messages(seededProduct) as ChatLoad.Success
        val card = repository.latestProposal(loaded.messages)

        assertEquals(MessageAuthor.SYSTEM, loaded.messages.last().author)
        assertEquals("Library", card?.point?.name)
        assertEquals(ProposalStatus.PENDING, card?.status)
        assertEquals("prop-1", card?.remoteId)
    }

    @Test
    fun `a dropped connection is reported as offline, not as an empty chat`() = runTest {
        messages.failure = IOException("no network")

        assertEquals(ChatLoad.Offline, repository.messages(seededProduct))
    }

    @Test
    fun `sending reuses the room opened for the listing`() = runTest {
        repository.messages(seededProduct)
        val result = repository.send(seededProduct, "On my way") as SendResult.Success

        assertEquals(listOf("room-1" to "On my way"), messages.sent)
        assertEquals(1, rooms.openedMaterials.size)
        assertEquals(MessageAuthor.ME, result.message.author)
    }

    @Test
    fun `a listing with no backend id cannot send`() = runTest {
        assertEquals(SendResult.NotSynced, repository.send(localOnlyProduct, "hello"))
        assertTrue(messages.sent.isEmpty())
    }

    @Test
    fun `an empty thread has no proposal card`() = runTest {
        val loaded = repository.messages(seededProduct) as ChatLoad.Success

        assertNull(repository.latestProposal(loaded.messages))
    }

    @Test
    fun `a photo message carries the picture, not the link as text`() = runTest {
        messages.stored += message("m1", OTHER, "https://blob.campusswap.test/photo-1.jpg")

        val loaded = repository.messages(seededProduct) as ChatLoad.Success

        assertEquals("https://blob.campusswap.test/photo-1.jpg", loaded.messages.single().imageUrl)
    }

    @Test
    fun `an ordinary message is not mistaken for a photo`() = runTest {
        messages.stored += message("m1", OTHER, "See https://uniandes.edu.co for the course page")

        val loaded = repository.messages(seededProduct) as ChatLoad.Success

        assertNull(loaded.messages.single().imageUrl)
    }

    @Test
    fun `a photo that cannot be uploaded reports no url instead of a broken one`() = runTest {
        uploads.failure = IOException("no network")

        assertNull(repository.uploadPhoto(ByteArray(10), "image/jpeg"))
    }

    @Test
    fun `uploading returns the stored url`() = runTest {
        assertEquals("https://blob.campusswap.test/photo-1.jpg", repository.uploadPhoto(ByteArray(4), "image/png"))
        assertEquals(listOf(4 to "image/png"), uploads.uploaded)
    }
}
