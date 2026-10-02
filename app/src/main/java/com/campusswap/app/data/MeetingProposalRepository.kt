package com.campusswap.app.data

import com.campusswap.app.data.remote.CreateMeetingProposalRequest
import com.campusswap.app.data.remote.FreeSlotDto
import com.campusswap.app.data.remote.MeetingProposalDto
import com.campusswap.app.data.remote.MeetingProposalRemoteDataSource
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

sealed interface ProposalResult {
    data class Success(val proposal: MeetingProposal) : ProposalResult
    data object Offline : ProposalResult
    data object NotSynced : ProposalResult
    data class Rejected(val message: String?) : ProposalResult
}

class MeetingProposalRepository(
    private val remote: MeetingProposalRemoteDataSource,
    private val zone: ZoneId = ZoneId.of("America/Bogota"),
) {
    private val chatRooms = ConcurrentHashMap<String, String>()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)

    suspend fun freeSlots(productId: String): List<TimeSlot> = try {
        val room = chatRoomFor(productId)
        if (room == null) {
            emptyList()
        } else {
            val suggestions = remote.suggestions(room)
            val slots = suggestions.slots.orEmpty()
            val suggested = suggestions.suggested
            (listOfNotNull(suggested) + slots.filterNot { it == suggested }).map { it.toTimeSlot() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun propose(productId: String, point: MeetingPoint, slot: TimeSlot): ProposalResult {
        val startsAt = slot.startsAt ?: return ProposalResult.Rejected("Pick one of the free times from the server")
        val endsAt = slot.endsAt ?: return ProposalResult.Rejected("Pick one of the free times from the server")
        val pointId = SeedIds.meetingPoint(point.id) ?: point.id.takeIf(::isUuid) ?: return ProposalResult.NotSynced
        return call {
            val room = chatRoomFor(productId) ?: return@call ProposalResult.NotSynced
            val created = remote.propose(CreateMeetingProposalRequest(room, pointId, startsAt.toString(), endsAt.toString()))
            ProposalResult.Success(created.toProposal(point))
        }
    }

    suspend fun latest(productId: String): MeetingProposal? = try {
        val room = chatRoomFor(productId)
        room?.let { id -> current(remote.proposals(id))?.let { dto -> dto.meetingPoint?.let { dto.toProposal(it.toMeetingPoint()) } } }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun respond(proposal: MeetingProposal, accept: Boolean): ProposalResult {
        val id = proposal.remoteId ?: return ProposalResult.NotSynced
        return call { ProposalResult.Success(remote.respond(id, accept).toProposal(proposal.point)) }
    }

    private fun current(proposals: List<MeetingProposalDto>): MeetingProposalDto? {
        val newestFirst = proposals.filter { it.status != "CANCELLED" }.sortedByDescending { it.createdAt ?: it.startsAt }
        return newestFirst.firstOrNull { it.status == "PENDING" }
            ?: newestFirst.firstOrNull { it.status == "ACCEPTED" }
            ?: newestFirst.firstOrNull()
    }

    private suspend fun chatRoomFor(productId: String): String? {
        chatRooms[productId]?.let { return it }
        val materialId = SeedIds.material(productId) ?: productId.takeIf(::isUuid) ?: return null
        return remote.openChatRoom(materialId).also { chatRooms[productId] = it }
    }

    private suspend fun call(block: suspend () -> ProposalResult): ProposalResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ProposalResult.Offline
    } catch (e: HttpException) {
        ProposalResult.Rejected(errorMessage(e))
    }

    private fun MeetingProposalDto.toProposal(point: MeetingPoint) = MeetingProposal(
        point = point,
        slot = FreeSlotDto(startsAt, endsAt, sharedBreak = false).toTimeSlot(),
        status = when (status) {
            "ACCEPTED" -> ProposalStatus.ACCEPTED
            "DECLINED" -> ProposalStatus.DECLINED
            else -> ProposalStatus.PENDING
        },
        remoteId = id,
        proposerId = proposerId,
    )

    private fun FreeSlotDto.toTimeSlot(): TimeSlot {
        val start = Instant.parse(startsAt)
        val end = Instant.parse(endsAt)
        return TimeSlot(
            id = startsAt,
            label = "${timeFormat.format(start.atZone(zone))} – ${timeFormat.format(end.atZone(zone))}",
            day = dayFormat.format(start.atZone(zone)),
            isSharedBreak = sharedBreak,
            startsAt = start,
            endsAt = end,
        )
    }

    private fun errorMessage(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string() ?: return null
        val message = JsonParser.parseString(body).asJsonObject.get("message")
        if (message.isJsonArray) message.asJsonArray.joinToString { it.asString } else message.asString
    }.getOrNull()

    private fun isUuid(id: String) = UUID_PATTERN.matches(id)

    private companion object {
        val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
