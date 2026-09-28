package com.campusswap.app.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Proximity {
    /** Roughly a building entrance plus typical phone GPS error outdoors. */
    const val CONFIRMATION_RADIUS_METERS = 50.0

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle (haversine) distance. Flat-earth math would be fine at campus scale too, but this is exact and just as cheap. */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun isWithinRadius(current: GeoPoint, target: GeoPoint, radiusMeters: Double = CONFIRMATION_RADIUS_METERS): Boolean =
        distanceMeters(current, target) <= radiusMeters
}
