package com.campusswap.app.screens.meeting

import com.campusswap.app.domain.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class CampusMapProjectionTest {
    @Test fun northWestCornerIsTopLeftAndSouthEastIsBottomRight() {
        val northWest = GeoPoint(4.605, -74.066)
        val southEast = GeoPoint(4.601, -74.063)
        val projection = CampusMapProjection(listOf(northWest, southEast), padding = 0.1f)

        assertPosition(0.1f, 0.1f, projection.project(northWest))
        assertPosition(0.9f, 0.9f, projection.project(southEast))
    }

    @Test fun singlePointDoesNotDivideByZero() {
        val only = GeoPoint(4.6, -74.06)

        assertPosition(0.14f, 0.14f, CampusMapProjection(listOf(only)).project(only))
    }

    private fun assertPosition(x: Float, y: Float, actual: MapPosition) {
        assertEquals(x, actual.x, 1e-5f)
        assertEquals(y, actual.y, 1e-5f)
    }
}
