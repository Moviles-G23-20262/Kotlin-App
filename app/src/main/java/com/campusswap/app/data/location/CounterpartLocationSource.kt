package com.campusswap.app.data.location

import com.campusswap.app.data.MeetingProposal
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.ProposalStatus
import com.campusswap.app.domain.GeoPoint

/**
 * Where the other student is expected to be. The backend has no live location
 * for a user, so the only real anchor is a meeting point both sides have agreed
 * on, or one the counterpart proposed. Null means it is still unknown.
 */
interface CounterpartLocationSource {
    suspend fun locationOf(productId: String): GeoPoint?
}

class ProposedMeetingLocation(
    private val proposals: MeetingProposalRepository,
    private val currentUserId: () -> String?,
) : CounterpartLocationSource {

    override suspend fun locationOf(productId: String): GeoPoint? =
        proposals.latest(productId)?.takeIf(::anchorsCounterpart)?.point?.location

    private fun anchorsCounterpart(proposal: MeetingProposal): Boolean = when (proposal.status) {
        ProposalStatus.ACCEPTED -> true
        ProposalStatus.PENDING, ProposalStatus.CHANGED ->
            proposal.proposerId != null && proposal.proposerId != currentUserId()
        ProposalStatus.DECLINED -> false
    }
}
