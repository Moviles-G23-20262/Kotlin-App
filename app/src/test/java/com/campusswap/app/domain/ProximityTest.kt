package com.campusswap.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximityTest {
    private val point = GeoPoint(10.0, 20.0)

    @Test fun samePointIsZeroMeters() {
        assertEquals(0.0, Proximity.distanceMeters(point, point), 1e-9)
    }

    @Test fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111_195.0, Proximity.distanceMeters(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0)), 1.0)
    }

    @Test fun pointsInsideTheRadiusAreAccepted() {
        assertTrue(Proximity.isWithinRadius(north(point, 30.0), point))
        assertTrue(Proximity.isWithinRadius(north(point, 49.9), point))
    }

    @Test fun pointsOutsideTheRadiusAreRejected() {
        assertFalse(Proximity.isWithinRadius(north(point, 50.1), point))
        assertFalse(Proximity.isWithinRadius(north(point, 250.0), point))
    }

    @Test fun distanceWrapsAroundTheAntimeridian() {
        val east = GeoPoint(0.0, 179.9995)
        val west = GeoPoint(0.0, -179.9995)
        assertEquals(111.2, Proximity.distanceMeters(east, west), 0.1)
    }
}

/** Moves [meters] due north; one degree of latitude is ~111,195 m on a 6,371 km sphere. */
fun north(from: GeoPoint, meters: Double) = GeoPoint(from.lat + meters / 111_194.93, from.lng)
