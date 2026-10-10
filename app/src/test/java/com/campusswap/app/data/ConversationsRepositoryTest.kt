package com.campusswap.app.data

import com.campusswap.app.data.materials.MaterialDto
import com.campusswap.app.data.remote.ChatPartyDto
import com.campusswap.app.data.remote.ChatRoomSummaryDto
import com.campusswap.app.data.remote.MessageDto
import com.campusswap.app.data.remote.UnreadCountDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ConversationsRepositoryTest {
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val them = "a0000000-0000-4000-8000-000000000001"

    private val remote = FakeChatRoomRemoteDataSource()
    private val repository = ConversationsRepository(remote, currentUserId = { me })

    private fun room(
        id: String,
        sellerId: String,
        title: String,
        lastText: String?,
        sentAt: String,
        unread: Int,
    ) = ChatRoomSummaryDto(
        id = id,
        materialId = "b0000000-0000-4000-8000-00000000000$id",
        buyerId = if (sellerId == me) them else me,
        sellerId = sellerId,
        material = MaterialDto("m-$id", title, "", null, "1000", null, "AVAILABLE", "SUPPLIES", sellerId),
        buyer = ChatPartyDto(if (sellerId == me) them else me, "Buyer Name"),
        seller = ChatPartyDto(sellerId, "Seller Name"),
        messages = lastText?.let {
            listOf(MessageDto("msg-$id", id, them, it, "TEXT", false, sentAt, null, null))
        },
        count = UnreadCountDto(unread),
    )

    @Test
    fun `a thread on my own listing is marked as selling`() = runTest {
        remote.summaries += room("1", sellerId = me, "Arduino UNO", "Is it still available?", "2026-10-05T17:00:00Z", 2)

        val conversation = (repository.conversations() as ConversationsLoad.Success).conversations.single()

        assertTrue(conversation.sellingThis)
        assertEquals("Buyer Name", conversation.counterpartName)
        assertEquals("Arduino UNO", conversation.productTitle)
        assertEquals(2, conversation.unread)
    }

    @Test
    fun `a thread on someone elses listing shows the seller`() = runTest {
        remote.summaries += room("1", sellerId = them, "Calculus 9th Ed.", "Sure, see you there", "2026-10-05T17:00:00Z", 0)

        val conversation = (repository.conversations() as ConversationsLoad.Success).conversations.single()

        assertFalse(conversation.sellingThis)
        assertEquals("Seller Name", conversation.counterpartName)
    }

    @Test
    fun `the newest conversation comes first`() = runTest {
        remote.summaries += room("1", them, "Old", "first", "2026-10-01T17:00:00Z", 0)
        remote.summaries += room("2", them, "New", "latest", "2026-10-05T17:00:00Z", 0)

        val conversations = (repository.conversations() as ConversationsLoad.Success).conversations

        assertEquals(listOf("New", "Old"), conversations.map { it.productTitle })
    }

    @Test
    fun `a thread with no messages yet still appears`() = runTest {
        remote.summaries += room("1", them, "Lab kit", lastText = null, sentAt = "", unread = 0)

        val conversation = (repository.conversations() as ConversationsLoad.Success).conversations.single()

        assertEquals(null, conversation.lastMessage)
        assertEquals("", conversation.time)
    }

    @Test
    fun `the unread badge adds up every thread`() = runTest {
        remote.summaries += room("1", me, "A", "x", "2026-10-05T17:00:00Z", 2)
        remote.summaries += room("2", them, "B", "y", "2026-10-05T18:00:00Z", 3)

        assertEquals(5, repository.unreadCount())
    }

    @Test
    fun `without a connection the badge does not invent a number`() = runTest {
        remote.failure = IOException("offline")

        assertEquals(ConversationsLoad.Offline, repository.conversations())
        assertEquals(0, repository.unreadCount())
    }
}
