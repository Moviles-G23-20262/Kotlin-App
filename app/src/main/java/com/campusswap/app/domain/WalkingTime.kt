package com.campusswap.app.domain

import kotlin.math.ceil

object WalkingTime {
    /** ~4.5 km/h: an unhurried pace, accounting for the stairs and slopes on campus. */
    private const val METERS_PER_MINUTE = 75.0

    /** Footpaths aren't straight lines between buildings; ~1.3 is a common detour factor for pedestrian networks. */
    private const val DETOUR_FACTOR = 1.3

    /** Estimated walk in whole minutes, never less than one. */
    fun minutes(from: GeoPoint, to: GeoPoint): Int {
        val meters = Proximity.distanceMeters(from, to) * DETOUR_FACTOR
        return ceil(meters / METERS_PER_MINUTE).toInt().coerceAtLeast(1)
    }
}
