package com.campusswap.app.screens.meeting

import com.campusswap.app.domain.GeoPoint
import kotlin.math.cos

data class MapPosition(val x: Float, val y: Float)

/**
 * Projects campus coordinates onto the map canvas. The bounds are fixed to the
 * Universidad de los Andes campus so the drawn buildings and the markers share
 * one frame, and the campus keeps its real shape inside whatever canvas it gets.
 */
class CampusMapProjection(
    private val canvasAspect: Float = CAMPUS_MAP_ASPECT,
    private val padding: Float = 0.03f,
) {
    private val widthFraction: Float
    private val heightFraction: Float

    init {
        val usable = 1f - 2 * padding
        val campusAspect = (LNG_SPAN * METERS_PER_LNG) / (LAT_SPAN * METERS_PER_LAT)
        if (campusAspect < canvasAspect) {
            heightFraction = usable
            widthFraction = usable * (campusAspect / canvasAspect).toFloat()
        } else {
            widthFraction = usable
            heightFraction = usable * (canvasAspect / campusAspect).toFloat()
        }
    }

    fun project(point: GeoPoint): MapPosition = MapPosition(
        x = (1f - widthFraction) / 2f + widthFraction * ((point.lng - MIN_LNG) / LNG_SPAN).toFloat(),
        y = (1f - heightFraction) / 2f + heightFraction * ((MAX_LAT - point.lat) / LAT_SPAN).toFloat(),
    )

    companion object {
        const val CAMPUS_MAP_ASPECT = 1.0f

        const val MIN_LAT = 4.59972
        const val MAX_LAT = 4.60517
        const val MIN_LNG = -74.06720
        const val MAX_LNG = -74.06308

        private const val LAT_SPAN = MAX_LAT - MIN_LAT
        private const val LNG_SPAN = MAX_LNG - MIN_LNG
        private const val METERS_PER_LAT = 111_320.0
        private val METERS_PER_LNG = 111_320.0 * cos(Math.toRadians((MIN_LAT + MAX_LAT) / 2))
    }
}
