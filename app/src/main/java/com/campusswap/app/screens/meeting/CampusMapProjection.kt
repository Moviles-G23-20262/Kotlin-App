package com.campusswap.app.screens.meeting

import com.campusswap.app.domain.GeoPoint

/** Normalised 0..1 position on the stylised campus map; y grows downwards like screen coordinates. */
data class MapPosition(val x: Float, val y: Float)

/**
 * Fits real coordinates into the stylised map by stretching their bounding box, so every point and both
 * people are always visible. It keeps relative positions, not true scale.
 */
class CampusMapProjection(points: List<GeoPoint>, private val padding: Float = 0.14f) {
    private val minLat = points.minOf { it.lat }
    private val maxLat = points.maxOf { it.lat }
    private val minLng = points.minOf { it.lng }
    // A single point (or a line) would make a span of zero; keep a small floor so we never divide by it.
    private val latSpan = (maxLat - minLat).coerceAtLeast(MIN_SPAN_DEGREES)
    private val lngSpan = (points.maxOf { it.lng } - minLng).coerceAtLeast(MIN_SPAN_DEGREES)

    fun project(point: GeoPoint): MapPosition {
        val usable = 1f - 2 * padding
        return MapPosition(
            x = padding + ((point.lng - minLng) / lngSpan).toFloat() * usable,
            y = padding + ((maxLat - point.lat) / latSpan).toFloat() * usable,
        )
    }

    private companion object {
        const val MIN_SPAN_DEGREES = 0.0005 // ~55 m
    }
}
