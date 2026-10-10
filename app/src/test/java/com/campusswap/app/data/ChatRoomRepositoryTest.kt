package com.campusswap.app.data

import com.campusswap.app.data.materials.MaterialDto
import com.campusswap.app.data.remote.ChatPartyDto
import com.campusswap.app.data.remote.ChatRoomSummaryDto
import com.campusswap.app.data.remote.UnreadCountDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRoomRepositoryTest {
    private val remote = FakeChatRoomRemoteDataSource()
    private val repository = ChatRoomRepository(remote)

    private val material = "b0000000-0000-4000-8000-000000000001"

    private fun existingRoom() = ChatRoomSummaryDto(
        id = "room-existing",
        materialId = material,
        buyerId = "a0000000-0000-4000-8000-000000000001",
        sellerId = "a0000000-0000-4000-8000-000000000000",
        material = MaterialDto("m", "QA", "", null, "1", null, "AVAILABLE", "OTHER", "s"),
        buyer = ChatPartyDto("b", "Buyer"),
        seller = ChatPartyDto("s", "Seller"),
        messages = null,
        count = UnreadCountDto(0),
    )

    @Test
    fun `an existing conversation is reused instead of opening another`() = runTest {
        remote.summaries += existingRoom()

        assertEquals("room-existing", repository.roomFor("p1"))
        assertEquals(0, remote.openCalls)
    }

    @Test
    fun `the first buyer to write opens the conversation`() = runTest {
        assertEquals("room-1", repository.roomFor("p1"))
        assertEquals(1, remote.openCalls)
    }

    @Test
    fun `the room is resolved once and then remembered`() = runTest {
        repository.roomFor("p1")
        repository.roomFor("p1")

        assertEquals(1, remote.openCalls)
    }

    @Test
    fun `a listing that is not on the server has no room`() = runTest {
        assertNull(repository.roomFor("local-4"))
        assertEquals(0, remote.openCalls)
    }
}
