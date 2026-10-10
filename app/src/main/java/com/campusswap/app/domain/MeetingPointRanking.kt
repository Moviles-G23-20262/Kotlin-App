package com.campusswap.app.domain

import kotlin.math.max

data class ZoneCandidate(val id: String, val isMonitored: Boolean, val location: GeoPoint)

data class RankedZone(val zone: ZoneCandidate, val walkMinutesMe: Int?, val walkMinutesOther: Int?) {
    val longestWalk: Int get() = max(walkMinutesMe ?: 0, walkMinutesOther ?: 0)
    val totalWalk: Int get() = (walkMinutesMe ?: 0) + (walkMinutesOther ?: 0)
}

enum class RankingMode { DAYTIME, NIGHT_SAFETY }

interface MeetingPointRankingStrategy {
    val mode: RankingMode

    fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint?): List<RankedZone>
}

class DaytimeStrategy : MeetingPointRankingStrategy {
    override val mode = RankingMode.DAYTIME

    override fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint?) = rankByLongestWalk(zones, me, other)
}

class NightSafetyStrategy : MeetingPointRankingStrategy {
    override val mode = RankingMode.NIGHT_SAFETY

    override fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint?) =
        rankByLongestWalk(zones.filter { it.isMonitored }, me, other)
}

private fun rankByLongestWalk(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint?): List<RankedZone> =
    zones
        .map { zone ->
            RankedZone(
                zone = zone,
                walkMinutesMe = me?.let { WalkingTime.minutes(it, zone.location) },
                walkMinutesOther = other?.let { WalkingTime.minutes(it, zone.location) },
            )
        }
        .sortedWith(compareBy({ it.longestWalk }, { it.totalWalk }, { !it.zone.isMonitored }, { it.zone.id }))
