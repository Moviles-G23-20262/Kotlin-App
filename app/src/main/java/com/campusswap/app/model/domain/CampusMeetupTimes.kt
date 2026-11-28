package com.campusswap.app.model.domain

import com.campusswap.app.model.MeetingProposalMapper
import com.campusswap.app.model.TimeSlot
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MeetupDay(val label: String, val slots: List<TimeSlot>)

object CampusMeetupTimes {
    private const val FIRST_START_HOUR = 7
    private const val LAST_START_HOUR = 19
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    fun upcoming(clock: Clock, days: Int = 6, zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE): List<MeetupDay> {
        val earliest = clock.instant().plus(Duration.ofMinutes(30))
        val today = LocalDate.now(clock.withZone(zone))
        return generateSequence(today) { it.plusDays(1) }
            .take(days + 2)
            .filter { it.dayOfWeek != DayOfWeek.SUNDAY }
            .map { date ->
                val slots = (FIRST_START_HOUR..LAST_START_HOUR).mapNotNull { hour ->
                    val start = date.atTime(hour, 0).atZone(zone)
                    if (start.toInstant().isBefore(earliest)) return@mapNotNull null
                    val end = start.plusHours(1)
                    TimeSlot(
                        id = start.toInstant().toString(),
                        label = "${timeFormat.format(start)} – ${timeFormat.format(end)}",
                        day = dayFormat.format(start),
                        isSharedBreak = false,
                        startsAt = start.toInstant(),
                        endsAt = end.toInstant(),
                    )
                }
                MeetupDay(dayFormat.format(date), slots)
            }
            .filter { it.slots.isNotEmpty() }
            .take(days)
            .toList()
    }
}
