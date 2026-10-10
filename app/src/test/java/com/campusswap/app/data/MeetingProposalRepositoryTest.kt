package com.campusswap.app.data

import com.campusswap.app.data.remote.FreeSlotDto
import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.data.remote.SlotSuggestionsDto
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Instant

class MeetingProposalRepositoryTest {
    private val remote = FakeMeetingProposalRemoteDataSource()
    private val chatRooms = FakeChatRoomRemoteDataSource()
    private val repository = MeetingProposalRepository(ChatRoomRepository(chatRooms), remote)
    private val pointId = "c0000000-0000-4000-8000-000000000001"
    private val point = MeetingPoint(pointId, "Library", "", MeetingZoneType.LIBRARY, true, 0, 0, 0f, 0f, GeoPoint(4.6, -74.06))
    private val noon = FreeSlotDto("2026-10-05T17:00:00Z", "2026-10-05T18:00:00Z", sharedBreak = true)
    private val morning = FreeSlotDto("2026-10-05T14:00:00Z", "2026-10-05T15:00:00Z", sharedBreak = false)

    private fun proposalDto(id: String, status: String, createdAt: String) = MeetingProposalDto(
        id, "seller-1", noon.startsAt, noon.endsAt, status, createdAt,
        MeetingPointDto(pointId, "Library", null, "LIBRARY", true, 4.6, -74.06),
    )

    @Test fun freeSlotsPutTheSuggestionFirstAndUseBogotaTime() = runBlocking {
        remote.suggestions = SlotSuggestionsDto(listOf(morning, noon), suggested = noon)

        val slots = repository.freeSlots("p1")

        assertEquals(listOf(noon.startsAt, morning.startsAt), slots.map { it.id })
        assertEquals("12:00 – 13:00", slots.first().label)
        assertEquals("Mon 5 Oct", slots.first().day)
        assertTrue(slots.first().isSharedBreak)
        assertEquals(listOf("b0000000-0000-4000-8000-000000000001"), chatRooms.openedMaterials)
    }

    @Test fun freeSlotsAreEmptyWhenTheServerIsUnreachable() = runBlocking {
        remote.failure = IOException("offline")

        assertTrue(repository.freeSlots("p1").isEmpty())
    }

    @Test fun proposeSendsTheRoomPointAndExactTimes() = runBlocking {
        remote.suggestions = SlotSuggestionsDto(listOf(noon), suggested = noon)
        val slot = repository.freeSlots("p1").single()

        val result = repository.propose("p1", point, slot)

        val request = remote.created.single()
        assertEquals("room-1", request.chatRoomId)
        assertEquals(pointId, request.meetingPointId)
        assertEquals(Instant.parse(noon.startsAt).toString(), request.startsAt)
        assertEquals("proposal-1", (result as ProposalResult.Success).proposal.remoteId)
        assertEquals(ProposalStatus.PENDING, result.proposal.status)
    }

    @Test fun aSlotWithoutServerTimesIsNotSent() = runBlocking {
        val result = repository.propose("p1", point, TimeSlot("t1", "10:00 – 11:00", "Wed", true))

        assertTrue(result is ProposalResult.Rejected)
        assertTrue(remote.created.isEmpty())
    }

    @Test fun serverValidationMessageIsShown() = runBlocking {
        remote.suggestions = SlotSuggestionsDto(listOf(noon), suggested = noon)
        val slot = repository.freeSlots("p1").single()
        val body = ResponseBody.create(MediaType.parse("application/json"), """{"message":"The meeting has to be in the future"}""")
        remote.failure = HttpException(Response.error<Any>(400, body))

        assertEquals(ProposalResult.Rejected("The meeting has to be in the future"), repository.propose("p1", point, slot))
    }

    @Test fun listingOnlyOnThisDeviceCannotBeProposed() = runBlocking {
        val slot = TimeSlot("s", "x", "y", false, Instant.parse(noon.startsAt), Instant.parse(noon.endsAt))

        assertEquals(ProposalResult.NotSynced, repository.propose("local-13", point, slot))
    }

    @Test fun latestIgnoresCancelledProposals() = runBlocking {
        remote.proposals = mutableListOf(
            proposalDto("old", "DECLINED", "2026-10-01T10:00:00Z"),
            proposalDto("withdrawn", "CANCELLED", "2026-10-02T10:00:00Z"),
        )

        val latest = repository.latest("p1")

        assertEquals("old", latest?.remoteId)
        assertEquals(ProposalStatus.DECLINED, latest?.status)
        assertEquals("seller-1", latest?.proposerId)
    }

    @Test fun aDeclinedCounterProposalKeepsTheAgreedMeetupVisible() = runBlocking {
        remote.proposals = mutableListOf(
            proposalDto("agreed", "ACCEPTED", "2026-10-01T10:00:00Z"),
            proposalDto("counter", "DECLINED", "2026-10-02T10:00:00Z"),
        )

        assertEquals("agreed", repository.latest("p1")?.remoteId)
    }

    @Test fun aPendingProposalIsShownOverAnOlderAgreement() = runBlocking {
        remote.proposals = mutableListOf(
            proposalDto("agreed", "ACCEPTED", "2026-10-01T10:00:00Z"),
            proposalDto("new", "PENDING", "2026-10-02T10:00:00Z"),
        )

        assertEquals("new", repository.latest("p1")?.remoteId)
    }

    @Test fun latestIsNullWhenTheServerFails() = runBlocking {
        remote.failure = IOException("offline")

        assertNull(repository.latest("p1"))
    }

    @Test fun acceptingSendsTheAnswerForThatProposal() = runBlocking {
        remote.proposals = mutableListOf(proposalDto("proposal-9", "PENDING", "2026-10-02T10:00:00Z"))
        val pending = repository.latest("p1")!!

        val result = repository.respond(pending, accept = true)

        assertEquals(listOf("proposal-9" to true), remote.responses)
        assertEquals(ProposalStatus.ACCEPTED, (result as ProposalResult.Success).proposal.status)
    }
}
