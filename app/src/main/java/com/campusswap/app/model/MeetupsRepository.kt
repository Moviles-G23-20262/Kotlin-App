package com.campusswap.app.model

import com.campusswap.app.model.local.MeetupDao
import com.campusswap.app.model.local.MeetupEntity
import com.campusswap.app.model.remote.ChatRoomRemoteDataSource
import com.campusswap.app.model.remote.ChatRoomSummaryDto
import com.campusswap.app.model.remote.FreeSlotDto
import com.campusswap.app.model.remote.MeetingProposalDto
import com.campusswap.app.model.remote.MeetingProposalRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

data class Meetup(
    val proposalId: String,
    val roomId: String,
    val productId: String,
    val productTitle: String,
    val counterpartName: String,
    val pointName: String,
    val slot: TimeSlot,
    val confirmed: Boolean,
    val proposedByMe: Boolean,
)

sealed interface MeetupsLoad {
    data class Success(val meetups: List<Meetup>, val savedAtMillis: Long? = null) : MeetupsLoad
    data object Offline : MeetupsLoad
}

class MeetupsRepository(
    private val rooms: ChatRoomRemoteDataSource,
    private val proposals: MeetingProposalRemoteDataSource,
    private val dao: MeetupDao,
    private val currentUserId: () -> String?,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE,
) {
    private val permits = Semaphore(MAX_PARALLEL)

    suspend fun saved(): MeetupsLoad.Success? {
        val owner = currentUserId() ?: return null
        val rows = dao.forOwner(owner)
        if (rows.isEmpty()) return null
        return MeetupsLoad.Success(upcoming(rows), rows.first().savedAtMillis)
    }

    suspend fun load(): MeetupsLoad {
        val owner = currentUserId() ?: return MeetupsLoad.Offline
        return try {
            val rows = fetch(owner)
            dao.replace(owner, rows)
            MeetupsLoad.Success(upcoming(rows))
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            saved() ?: MeetupsLoad.Offline
        } catch (e: HttpException) {
            saved() ?: MeetupsLoad.Offline
        }
    }

    private suspend fun fetch(owner: String): List<MeetupEntity> {
        val now = clock.millis()
        val all = rooms.rooms()
        val perRoom = coroutineScope {
            all.map { room -> async { permits.withPermit { room to proposals.proposals(room.id) } } }.awaitAll()
        }
        return perRoom.flatMap { (room, list) -> list.mapNotNull { toEntity(owner, room, it, now) } }
    }

    private fun toEntity(owner: String, room: ChatRoomSummaryDto, dto: MeetingProposalDto, now: Long): MeetupEntity? {
        if (dto.status != ACCEPTED && dto.status != PENDING) return null
        val endsAt = Instant.parse(dto.endsAt).toEpochMilli()
        if (endsAt <= now) return null
        val selling = room.sellerId == owner
        return MeetupEntity(
            ownerId = owner,
            proposalId = dto.id,
            roomId = room.id,
            productId = room.materialId,
            productTitle = room.material?.title ?: "This listing",
            counterpartName = (if (selling) room.buyer else room.seller)?.fullName ?: "A student",
            pointName = dto.meetingPoint?.name ?: "Meeting point",
            startsAtMillis = Instant.parse(dto.startsAt).toEpochMilli(),
            endsAtMillis = endsAt,
            confirmed = dto.status == ACCEPTED,
            proposedByMe = dto.proposerId == owner,
            savedAtMillis = now,
        )
    }

    private fun upcoming(rows: List<MeetupEntity>): List<Meetup> {
        val now = clock.millis()
        return rows.filter { it.endsAtMillis > now }.sortedBy { it.startsAtMillis }.map(::toMeetup)
    }

    private fun toMeetup(row: MeetupEntity) = Meetup(
        proposalId = row.proposalId,
        roomId = row.roomId,
        productId = row.productId,
        productTitle = row.productTitle,
        counterpartName = row.counterpartName,
        pointName = row.pointName,
        slot = MeetingProposalMapper.toTimeSlot(
            FreeSlotDto(Instant.ofEpochMilli(row.startsAtMillis).toString(), Instant.ofEpochMilli(row.endsAtMillis).toString(), sharedBreak = false),
            zone,
        ),
        confirmed = row.confirmed,
        proposedByMe = row.proposedByMe,
    )

    private companion object {
        const val MAX_PARALLEL = 4
        const val ACCEPTED = "ACCEPTED"
        const val PENDING = "PENDING"
    }
}
