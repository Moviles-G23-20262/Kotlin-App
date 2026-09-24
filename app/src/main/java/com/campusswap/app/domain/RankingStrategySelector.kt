package com.campusswap.app.domain

import java.time.LocalTime

fun interface RankingStrategySelector {
    fun strategyFor(now: LocalTime): MeetingPointRankingStrategy
}

class TimeOfDayStrategySelector(
    private val daytime: MeetingPointRankingStrategy = DaytimeStrategy(),
    private val night: MeetingPointRankingStrategy = NightSafetyStrategy(),
) : RankingStrategySelector {

    override fun strategyFor(now: LocalTime): MeetingPointRankingStrategy = if (isNight(now)) night else daytime

    private fun isNight(now: LocalTime) = !now.isBefore(NIGHT_START) || now.isBefore(NIGHT_END)

    companion object {
        val NIGHT_START: LocalTime = LocalTime.of(18, 0)
        val NIGHT_END: LocalTime = LocalTime.of(6, 0)
    }
}
