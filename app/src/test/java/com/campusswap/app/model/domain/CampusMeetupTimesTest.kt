package com.campusswap.app.model.domain

import com.campusswap.app.model.MeetingPoint
import com.campusswap.app.model.MeetingZoneType
import com.campusswap.app.model.orders.PaymentMethod
import com.campusswap.app.viewmodel.CheckoutUiState
import com.campusswap.app.viewmodel.MeetupChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class CampusMeetupTimesTest {
    private val zone = ZoneId.of("America/Bogota")
    private val saturdayAt6pm = Clock.fixed(Instant.parse("2026-11-28T23:00:00Z"), zone)

    @Test
    fun `skips sundays and hours that already passed`() {
        val days = CampusMeetupTimes.upcoming(saturdayAt6pm, days = 3)

        assertEquals(3, days.size)
        assertEquals(listOf("19:00 – 20:00"), days[0].slots.map { it.label })
        assertTrue(days.all { day -> day.slots.all { it.startsAt!!.atZone(zone).dayOfWeek != DayOfWeek.SUNDAY } })
        assertEquals(13, days[1].slots.size)
    }

    @Test
    fun `a time picked for one item is not offered to the others`() {
        val slot = CampusMeetupTimes.upcoming(saturdayAt6pm).first().slots.first()
        val point = MeetingPoint("p", "Library", "", MeetingZoneType.LIBRARY, true, 0, 0, 0f, 0f)
        val state = CheckoutUiState(
            choices = mapOf(
                "book" to MeetupChoice(point, 0, slot, PaymentMethod.NEQUI),
                "calculator" to MeetupChoice(point, 0, null, PaymentMethod.CASH),
            ),
        )

        assertEquals(setOf(slot.id), state.takenByOthers("calculator"))
        assertTrue(state.takenByOthers("book").isEmpty())
        assertFalse(state.isComplete(listOf("book", "calculator")))
        assertEquals(setOf("book"), state.plans().keys)
    }
}
