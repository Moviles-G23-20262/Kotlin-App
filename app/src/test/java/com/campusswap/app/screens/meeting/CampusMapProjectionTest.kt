package com.campusswap.app.screens.meeting

import com.campusswap.app.domain.GeoPoint
import com.campusswap.app.screens.meeting.CampusMapProjection.Companion.MAX_LAT
import com.campusswap.app.screens.meeting.CampusMapProjection.Companion.MAX_LNG
import com.campusswap.app.screens.meeting.CampusMapProjection.Companion.MIN_LAT
import com.campusswap.app.screens.meeting.CampusMapProjection.Companion.MIN_LNG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusMapProjectionTest {
    private val projection = CampusMapProjection()

    @Test fun theCampusIsCentredOnTheCanvas() {
        val centre = projection.project(GeoPoint((MIN_LAT + MAX_LAT) / 2, (MIN_LNG + MAX_LNG) / 2))

        assertEquals(0.5f, centre.x, 1e-5f)
        assertEquals(0.5f, centre.y, 1e-5f)
    }

    @Test fun northWestIsTopLeftAndSouthEastIsBottomRight() {
        val northWest = projection.project(GeoPoint(MAX_LAT, MIN_LNG))
        val southEast = projection.project(GeoPoint(MIN_LAT, MAX_LNG))

        assertTrue(northWest.x < southEast.x)
        assertTrue(northWest.y < southEast.y)
        assertEquals(1f - southEast.x, northWest.x, 1e-5f)
        assertEquals(1f - southEast.y, northWest.y, 1e-5f)
    }

    @Test fun theCampusKeepsItsShapeInsteadOfStretchingToTheCanvas() {
        val widthOnCanvas = projection.project(GeoPoint(MIN_LAT, MAX_LNG)).x -
            projection.project(GeoPoint(MIN_LAT, MIN_LNG)).x
        val heightOnCanvas = projection.project(GeoPoint(MIN_LAT, MIN_LNG)).y -
            projection.project(GeoPoint(MAX_LAT, MIN_LNG)).y

        val drawnAspect = (widthOnCanvas * CampusMapProjection.CAMPUS_MAP_ASPECT) / heightOnCanvas
        assertEquals(0.77f, drawnAspect, 0.02f)
    }

    @Test fun everyCampusCornerStaysInsideTheCanvas() {
        listOf(
            GeoPoint(MIN_LAT, MIN_LNG), GeoPoint(MIN_LAT, MAX_LNG),
            GeoPoint(MAX_LAT, MIN_LNG), GeoPoint(MAX_LAT, MAX_LNG),
        ).forEach { corner ->
            val position = projection.project(corner)
            assertTrue(position.x in 0f..1f && position.y in 0f..1f)
        }
    }

    @Test fun aPointOutsideTheCampusStillProjectsWithoutCrashing() {
        val faraway = projection.project(GeoPoint(4.70, -74.10))

        assertTrue(faraway.x < 0f || faraway.y < 0f)
    }
}
