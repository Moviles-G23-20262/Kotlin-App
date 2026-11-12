package com.campusswap.app.model

import com.campusswap.app.model.local.FakeMeetupDao
import com.campusswap.app.model.remote.ChatPartyDto
import com.campusswap.app.model.remote.ChatRoomSummaryDto
import com.campusswap.app.model.remote.CreateMeetingProposalRequest
import com.campusswap.app.model.remote.MeetingPointDto
import com.campusswap.app.model.remote.MeetingProposalDto
import com.campusswap.app.model.remote.MeetingProposalRemoteDataSource
import com.campusswap.app.model.remote.SlotSuggestionsDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MeetupsRepositoryTest {
    private val rooms = FakeChatRoomRemoteDataSource()
    private val proposals = RoomProposals()
    private val dao = FakeMeetupDao()
    private val now = Instant.parse("2026-11-12T15:00:00Z")

    private fun repository(at: Instant = now, user: String = ME) =
        MeetupsRepository(rooms, proposals, dao, { user }, Clock.fixed(at, ZoneOffset.UTC))

    private class RoomProposals : MeetingProposalRemoteDataSource {
        val byRoom = mutableMapOf<String, List<MeetingProposalDto>>()
        var failure: Exception? = null
        var delayMs = 0L
        var inFlight = 0
        var maxInFlight = 0

        override suspend fun proposals(chatRoomId: String): List<MeetingProposalDto> {
            failure?.let { throw it }
            inFlight++
            maxInFlight = maxOf(maxInFlight, inFlight)
            delay(delayMs)
            inFlight--
            return byRoom[chatRoomId].orEmpty()
        }

        override suspend fun suggestions(chatRoomId: String): SlotSuggestionsDto = error("not used")
        override suspend fun propose(request: CreateMeetingProposalRequest): MeetingProposalDto = error("not used")
        override suspend fun respond(proposalId: String, accept: Boolean): MeetingProposalDto = error("not used")
    }

    private fun room(id: String, seller: String = "ana", buyer: String = ME) = ChatRoomSummaryDto(
        id = id,
        materialId = "material-$id",
        buyerId = buyer,
        sellerId = seller,
        material = null,
        buyer = ChatPartyDto(buyer, "Buyer $buyer"),
        seller = ChatPartyDto(seller, "Ana Ruiz"),
        messages = null,
        count = null,
    )

    private fun proposal(id: String, status: String, startsAt: String, proposer: String = "ana") = MeetingProposalDto(
        id = id,
        proposerId = proposer,
        startsAt = startsAt,
        endsAt = Instant.parse(startsAt).plusSeconds(3600).toString(),
        status = status,
        createdAt = null,
        meetingPoint = MeetingPointDto("point-1", "Central Library lobby", null, "LIBRARY", true, 4.6, -74.06),
    )

    @Test
    fun `lists the agreed and pending meetups of every room, soonest first`() = runTest {
        rooms.summaries += room("room-1")
        rooms.summaries += room("room-2", seller = ME, buyer = "luis")
        proposals.byRoom["room-1"] = listOf(
            proposal("later", "ACCEPTED", "2026-11-14T15:00:00Z"),
            proposal("declined", "DECLINED", "2026-11-13T15:00:00Z"),
            proposal("over", "ACCEPTED", "2026-11-10T15:00:00Z"),
        )
        proposals.byRoom["room-2"] = listOf(
            proposal("soon", "PENDING", "2026-11-12T18:00:00Z", proposer = ME),
            proposal("withdrawn", "CANCELLED", "2026-11-12T19:00:00Z"),
        )

        val meetups = (repository().load() as MeetupsLoad.Success).meetups

        assertEquals(listOf("soon", "later"), meetups.map { it.proposalId })
        assertEquals(listOf(false, true), meetups.map { it.confirmed })
        assertEquals(listOf(true, false), meetups.map { it.proposedByMe })
        assertEquals(listOf("Buyer luis", "Ana Ruiz"), meetups.map { it.counterpartName })
        assertEquals("Central Library lobby", meetups.first().pointName)
        assertEquals("13:00 – 14:00", meetups.first().slot.label)
    }

    @Test
    fun `rooms are asked in parallel, never more than four at a time`() = runTest {
        repeat(8) { rooms.summaries += room("room-$it") }
        proposals.delayMs = 100

        repository().load()

        assertEquals(4, proposals.maxInFlight)
        assertEquals(200, currentTime)
    }

    @Test
    fun `the agenda is saved so it opens from the phone`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "ACCEPTED", "2026-11-13T15:00:00Z"))
        repository().load()

        val saved = repository().saved()

        assertEquals(listOf("p1"), saved?.meetups?.map { it.proposalId })
        assertEquals(now.toEpochMilli(), saved?.savedAtMillis)
    }

    @Test
    fun `offline, the saved agenda is shown with its age`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "ACCEPTED", "2026-11-13T15:00:00Z"))
        repository().load()

        rooms.failure = IOException("airplane mode")
        val offline = repository(at = now.plusSeconds(600)).load() as MeetupsLoad.Success

        assertEquals(listOf("p1"), offline.meetups.map { it.proposalId })
        assertEquals(now.toEpochMilli(), offline.savedAtMillis)
    }

    @Test
    fun `offline with nothing saved there is no agenda to show`() = runTest {
        rooms.failure = IOException("airplane mode")

        assertEquals(MeetupsLoad.Offline, repository().load())
    }

    @Test
    fun `one room failing falls back to the saved agenda`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "ACCEPTED", "2026-11-13T15:00:00Z"))
        repository().load()

        proposals.failure = IOException("timeout")
        val offline = repository().load() as MeetupsLoad.Success

        assertEquals(listOf("p1"), offline.meetups.map { it.proposalId })
    }

    @Test
    fun `meetups that are over are hidden from the saved agenda`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "ACCEPTED", "2026-11-13T15:00:00Z"))
        repository().load()

        val nextWeek = repository(at = now.plusSeconds(7 * 24 * 3600)).saved()

        assertTrue(nextWeek?.meetups.orEmpty().isEmpty())
    }

    @Test
    fun `a meetup that was declined later disappears on the next refresh`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "PENDING", "2026-11-13T15:00:00Z"))
        repository().load()

        proposals.byRoom["room-1"] = listOf(proposal("p1", "DECLINED", "2026-11-13T15:00:00Z"))
        repository().load()

        assertNull(repository().saved())
    }

    @Test
    fun `another account on the phone never sees my agenda`() = runTest {
        rooms.summaries += room("room-1")
        proposals.byRoom["room-1"] = listOf(proposal("p1", "ACCEPTED", "2026-11-13T15:00:00Z"))
        repository().load()

        assertNull(repository(user = "someone-else").saved())
    }

    private companion object {
        const val ME = "me"
    }
}
