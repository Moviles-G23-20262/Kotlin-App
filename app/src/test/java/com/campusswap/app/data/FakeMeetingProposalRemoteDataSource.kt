package com.campusswap.app.data

import com.campusswap.app.data.remote.CreateMeetingProposalRequest
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.data.remote.MeetingProposalRemoteDataSource
import com.campusswap.app.data.remote.SlotSuggestionsDto

class FakeMeetingProposalRemoteDataSource : MeetingProposalRemoteDataSource {
    var suggestions = SlotSuggestionsDto(emptyList(), null)
    var proposals = mutableListOf<MeetingProposalDto>()
    var failure: Exception? = null
    val created = mutableListOf<CreateMeetingProposalRequest>()
    val responses = mutableListOf<Pair<String, Boolean>>()

    private fun check() = failure?.let { throw it }

    override suspend fun suggestions(chatRoomId: String): SlotSuggestionsDto {
        check()
        return suggestions
    }

    override suspend fun proposals(chatRoomId: String): List<MeetingProposalDto> {
        check()
        return proposals
    }

    override suspend fun propose(request: CreateMeetingProposalRequest): MeetingProposalDto {
        check()
        created += request
        return MeetingProposalDto("proposal-1", "buyer-1", request.startsAt, request.endsAt, "PENDING", null, null)
    }

    override suspend fun respond(proposalId: String, accept: Boolean): MeetingProposalDto {
        check()
        responses += proposalId to accept
        val current = proposals.first { it.id == proposalId }
        return current.copy(status = if (accept) "ACCEPTED" else "DECLINED")
    }
}
