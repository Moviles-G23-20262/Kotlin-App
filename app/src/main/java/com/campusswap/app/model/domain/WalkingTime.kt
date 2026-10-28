package com.campusswap.app.domain

import kotlin.math.ceil

object WalkingTime {
    private const val METERS_PER_MINUTE = 75.0
    private const val DETOUR_FACTOR = 1.3

    fun minutes(from: GeoPoint, to: GeoPoint): Int {
        val meters = Proximity.distanceMeters(from, to) * DETOUR_FACTOR
        return ceil(meters / METERS_PER_MINUTE).toInt().coerceAtLeast(1)
    }
}
