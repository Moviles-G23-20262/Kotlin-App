package com.campusswap.app.data.location

import com.campusswap.app.data.ChatRoomRepository
import com.campusswap.app.data.FakeChatRoomRemoteDataSource
import com.campusswap.app.data.FakeMeetingProposalRemoteDataSource
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.remote.MeetingPointDto
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProposedMeetingLocationTest {
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val them = "a0000000-0000-4000-8000-000000000001"
    private val library = MeetingPointDto("c0000000-0000-4000-8000-000000000001", "Library", null, "LIBRARY", true, 4.6, -74.06)

    private val remote = FakeMeetingProposalRemoteDataSource()
    private val source = ProposedMeetingLocation(
        MeetingProposalRepository(ChatRoomRepository(FakeChatRoomRemoteDataSource()), remote),
        currentUserId = { me },
    )

    private fun proposal(status: String, proposer: String) = MeetingProposalDto(
        "prop-1", proposer, "2026-10-05T17:00:00Z", "2026-10-05T18:00:00Z", status, "2026-10-05T16:00:00Z", library,
    )

    @Test
    fun `without a proposal the counterpart location is unknown`() = runTest {
        assertNull(source.locationOf("p1"))
    }

    @Test
    fun `an agreed meeting point anchors the counterpart`() = runTest {
        remote.proposals += proposal("ACCEPTED", them)

        assertEquals(GeoPoint(4.6, -74.06), source.locationOf("p1"))
    }

    @Test
    fun `a spot the counterpart proposed anchors them too`() = runTest {
        remote.proposals += proposal("PENDING", them)

        assertEquals(GeoPoint(4.6, -74.06), source.locationOf("p1"))
    }

    @Test
    fun `my own pending proposal says nothing about where they are`() = runTest {
        remote.proposals += proposal("PENDING", me)

        assertNull(source.locationOf("p1"))
    }

    @Test
    fun `a declined spot is not an anchor`() = runTest {
        remote.proposals += proposal("DECLINED", them)

        assertNull(source.locationOf("p1"))
    }

    @Test
    fun `a listing that is not on the server has no anchor`() = runTest {
        assertNull(source.locationOf("local-3"))
    }
}
