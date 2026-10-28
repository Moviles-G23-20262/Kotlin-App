package com.campusswap.app.data

import com.campusswap.app.data.remote.FreeSlotDto
import com.campusswap.app.data.remote.MeetingProposalDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MeetingProposalMapper {
    val CAMPUS_ZONE: ZoneId = ZoneId.of("America/Bogota")

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)

    fun toTimeSlot(slot: FreeSlotDto, zone: ZoneId = CAMPUS_ZONE): TimeSlot {
        val start = Instant.parse(slot.startsAt)
        val end = Instant.parse(slot.endsAt)
        return TimeSlot(
            id = slot.startsAt,
            label = "${timeFormat.format(start.atZone(zone))} – ${timeFormat.format(end.atZone(zone))}",
            day = dayFormat.format(start.atZone(zone)),
            isSharedBreak = slot.sharedBreak,
            startsAt = start,
            endsAt = end,
        )
    }

    fun toProposal(dto: MeetingProposalDto, point: MeetingPoint, zone: ZoneId = CAMPUS_ZONE) = MeetingProposal(
        point = point,
        slot = toTimeSlot(FreeSlotDto(dto.startsAt, dto.endsAt, sharedBreak = false), zone),
        status = toStatus(dto.status),
        remoteId = dto.id,
        proposerId = dto.proposerId,
    )

    fun toProposal(dto: MeetingProposalDto, zone: ZoneId = CAMPUS_ZONE): MeetingProposal? =
        dto.meetingPoint?.let { toProposal(dto, it.toMeetingPoint(), zone) }

    private fun toStatus(status: String?) = when (status) {
        "ACCEPTED" -> ProposalStatus.ACCEPTED
        "DECLINED" -> ProposalStatus.DECLINED
        else -> ProposalStatus.PENDING
    }
}
