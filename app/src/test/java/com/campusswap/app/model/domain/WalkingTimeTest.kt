package com.campusswap.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WalkingTimeTest {
    private val origin = GeoPoint(0.0, 0.0)

    @Test fun samePlaceStillCountsAsOneMinute() {
        assertEquals(1, WalkingTime.minutes(origin, origin))
    }

    @Test fun includesDetourAndRoundsUp() {
        assertEquals(2, WalkingTime.minutes(origin, north(origin, 100.0)))
        assertEquals(18, WalkingTime.minutes(origin, north(origin, 1000.0)))
    }
}
