package com.campusswap.app.screens.meeting

import com.campusswap.app.domain.GeoPoint

data class MapPosition(val x: Float, val y: Float)

class CampusMapProjection(points: List<GeoPoint>, private val padding: Float = 0.14f) {
    private val minLat = points.minOf { it.lat }
    private val maxLat = points.maxOf { it.lat }
    private val minLng = points.minOf { it.lng }
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
        const val MIN_SPAN_DEGREES = 0.0005
    }
}
