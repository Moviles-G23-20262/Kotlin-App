package com.campusswap.app.data.location

import com.campusswap.app.domain.GeoPoint

sealed interface LocationResult {
    data class Fix(val point: GeoPoint, val accuracyMeters: Float) : LocationResult
    /** Precise location not granted (denied, or the user picked "approximate"). */
    data object PermissionDenied : LocationResult
    /** Location services are switched off in system settings. */
    data object ProviderDisabled : LocationResult
    /** Services are on but no fix arrived in time (indoors, cold GPS). */
    data object Unavailable : LocationResult
}

interface LocationDataSource {
    suspend fun currentLocation(): LocationResult
}
