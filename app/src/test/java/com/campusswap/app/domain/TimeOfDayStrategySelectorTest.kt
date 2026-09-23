package com.campusswap.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayStrategySelectorTest {
    private val selector = TimeOfDayStrategySelector()

    private fun modeAt(hour: Int, minute: Int = 0) = selector.strategyFor(LocalTime.of(hour, minute)).mode

    @Test fun daytimeFromSixToJustBeforeSix() {
        assertEquals(RankingMode.DAYTIME, modeAt(6, 0))
        assertEquals(RankingMode.DAYTIME, modeAt(12, 0))
        assertEquals(RankingMode.DAYTIME, modeAt(17, 59))
    }

    @Test fun nightFromSixInTheEveningUntilSixInTheMorning() {
        assertEquals(RankingMode.NIGHT_SAFETY, modeAt(18, 0))
        assertEquals(RankingMode.NIGHT_SAFETY, modeAt(23, 30))
        assertEquals(RankingMode.NIGHT_SAFETY, modeAt(0, 0))
        assertEquals(RankingMode.NIGHT_SAFETY, modeAt(5, 59))
    }
}
