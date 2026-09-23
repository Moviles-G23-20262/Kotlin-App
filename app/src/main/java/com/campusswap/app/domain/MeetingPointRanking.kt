package com.campusswap.app.domain

import kotlin.math.max

/** A meeting point as the ranking sees it: only what the rules need. */
data class ZoneCandidate(val id: String, val isMonitored: Boolean, val location: GeoPoint)

/** [walkMinutesMe] is null when the user's own location is unknown. */
data class RankedZone(val zone: ZoneCandidate, val walkMinutesMe: Int?, val walkMinutesOther: Int) {
    val longestWalk: Int get() = max(walkMinutesMe ?: 0, walkMinutesOther)
    val totalWalk: Int get() = (walkMinutesMe ?: 0) + walkMinutesOther
}

enum class RankingMode { DAYTIME, NIGHT_SAFETY }

/** Strategy pattern: how Campus Guardian orders meeting points. Callers never depend on a concrete rule. */
interface MeetingPointRankingStrategy {
    val mode: RankingMode

    /** Best first. Zones the strategy doesn't allow are left out, so the list may be empty. */
    fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint): List<RankedZone>
}

/** Daylight: every public zone is acceptable; monitored ones only win ties. */
class DaytimeStrategy : MeetingPointRankingStrategy {
    override val mode = RankingMode.DAYTIME

    override fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint) = rankByLongestWalk(zones, me, other)
}

/** After dark: only zones with campus security cameras are suggested, even if an open plaza is closer. */
class NightSafetyStrategy : MeetingPointRankingStrategy {
    override val mode = RankingMode.NIGHT_SAFETY

    override fun rank(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint) =
        rankByLongestWalk(zones.filter { it.isMonitored }, me, other)
}

/**
 * Minimax: the best zone is the one where the person walking furthest walks the least, so neither
 * side is sent across campus. Ties go to the shorter combined walk, then to monitored zones.
 */
private fun rankByLongestWalk(zones: List<ZoneCandidate>, me: GeoPoint?, other: GeoPoint): List<RankedZone> =
    zones
        .map { zone ->
            RankedZone(
                zone = zone,
                walkMinutesMe = me?.let { WalkingTime.minutes(it, zone.location) },
                walkMinutesOther = WalkingTime.minutes(other, zone.location),
            )
        }
        .sortedWith(compareBy({ it.longestWalk }, { it.totalWalk }, { !it.zone.isMonitored }, { it.zone.id }))
