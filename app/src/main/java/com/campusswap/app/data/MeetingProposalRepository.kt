package com.campusswap.app.data

import com.campusswap.app.data.remote.ApiErrors
import com.campusswap.app.data.remote.CreateMeetingProposalRequest
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.data.remote.MeetingProposalRemoteDataSource
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId

sealed interface ProposalResult {
    data class Success(val proposal: MeetingProposal) : ProposalResult
    data object Offline : ProposalResult
    data object NotSynced : ProposalResult
    data class Rejected(val message: String?) : ProposalResult
}

class MeetingProposalRepository(
    private val rooms: ChatRoomRepository,
    private val remote: MeetingProposalRemoteDataSource,
    private val zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE,
) {

    suspend fun freeSlots(productId: String): List<TimeSlot> = try {
        val room = rooms.roomFor(productId)
        if (room == null) {
            emptyList()
        } else {
            val suggestions = remote.suggestions(room)
            val slots = suggestions.slots.orEmpty()
            val suggested = suggestions.suggested
            (listOfNotNull(suggested) + slots.filterNot { it == suggested })
                .map { MeetingProposalMapper.toTimeSlot(it, zone) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun propose(productId: String, point: MeetingPoint, slot: TimeSlot): ProposalResult {
        val startsAt = slot.startsAt ?: return ProposalResult.Rejected("Pick one of the free times from the server")
        val endsAt = slot.endsAt ?: return ProposalResult.Rejected("Pick one of the free times from the server")
        val pointId = SeedIds.backendMeetingPoint(point.id) ?: return ProposalResult.NotSynced
        return call {
            val room = rooms.roomFor(productId) ?: return@call ProposalResult.NotSynced
            val created = remote.propose(CreateMeetingProposalRequest(room, pointId, startsAt.toString(), endsAt.toString()))
            ProposalResult.Success(MeetingProposalMapper.toProposal(created, point, zone))
        }
    }

    suspend fun latest(productId: String): MeetingProposal? = try {
        val room = rooms.roomFor(productId)
        room?.let { id -> current(remote.proposals(id))?.let { MeetingProposalMapper.toProposal(it, zone) } }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun respond(proposal: MeetingProposal, accept: Boolean): ProposalResult {
        val id = proposal.remoteId ?: return ProposalResult.NotSynced
        return call {
            ProposalResult.Success(MeetingProposalMapper.toProposal(remote.respond(id, accept), proposal.point, zone))
        }
    }

    private fun current(proposals: List<MeetingProposalDto>): MeetingProposalDto? {
        val newestFirst = proposals.filter { it.status != "CANCELLED" }.sortedByDescending { it.createdAt ?: it.startsAt }
        return newestFirst.firstOrNull { it.status == "PENDING" }
            ?: newestFirst.firstOrNull { it.status == "ACCEPTED" }
            ?: newestFirst.firstOrNull()
    }

    private suspend fun call(block: suspend () -> ProposalResult): ProposalResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ProposalResult.Offline
    } catch (e: HttpException) {
        ProposalResult.Rejected(ApiErrors.message(e))
    }
}
