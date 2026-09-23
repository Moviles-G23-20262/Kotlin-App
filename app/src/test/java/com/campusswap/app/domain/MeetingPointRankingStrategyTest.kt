package com.campusswap.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingPointRankingStrategyTest {
    // Both parties on one north-south line: me at the origin, the other person 600 m north.
    private val me = GeoPoint(0.0, 0.0)
    private val other = north(me, 600.0)

    private val nearMe = ZoneCandidate("near-me", isMonitored = true, location = north(me, 50.0))       // 1 min vs 10 min
    private val middlePlaza = ZoneCandidate("middle", isMonitored = false, location = north(me, 300.0)) // 6 min each
    private val nearOther = ZoneCandidate("near-other", isMonitored = true, location = north(me, 450.0)) // 8 min vs 3 min
    private val zones = listOf(nearMe, middlePlaza, nearOther)

    @Test fun daytimeMinimisesTheLongestWalkNotTheSum() {
        val ranked = DaytimeStrategy().rank(zones, me, other)

        // near-me has the smallest total (11 min) but makes the other person walk 10; middle caps both at 6.
        assertEquals("middle", ranked.first().zone.id)
        assertEquals(6, ranked.first().walkMinutesMe)
        assertEquals(6, ranked.first().walkMinutesOther)
        assertEquals(listOf("middle", "near-other", "near-me"), ranked.map { it.zone.id })
    }

    @Test fun nightOnlySuggestsMonitoredZonesEvenIfFurther() {
        val ranked = NightSafetyStrategy().rank(zones, me, other)

        assertEquals(listOf("near-other", "near-me"), ranked.map { it.zone.id })
        assertTrue(ranked.all { it.zone.isMonitored })
    }

    @Test fun nightWithNoMonitoredZoneSuggestsNothing() {
        assertTrue(NightSafetyStrategy().rank(listOf(middlePlaza), me, other).isEmpty())
    }

    @Test fun daytimeTieIsBrokenInFavourOfMonitoredZones() {
        val plaza = ZoneCandidate("a-plaza", isMonitored = false, location = middlePlaza.location)
        val lobby = ZoneCandidate("b-lobby", isMonitored = true, location = middlePlaza.location)

        assertEquals("b-lobby", DaytimeStrategy().rank(listOf(plaza, lobby), me, other).first().zone.id)
    }

    @Test fun unknownOwnLocationRanksByTheOtherPersonsWalk() {
        val ranked = DaytimeStrategy().rank(zones, me = null, other = other)

        assertEquals("near-other", ranked.first().zone.id)
        assertEquals(null, ranked.first().walkMinutesMe)
    }
}
