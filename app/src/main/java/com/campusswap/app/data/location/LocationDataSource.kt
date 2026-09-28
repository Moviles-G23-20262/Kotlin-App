package com.campusswap.app.data.location

import com.campusswap.app.domain.GeoPoint

sealed interface LocationResult {
    data class Fix(val point: GeoPoint, val accuracyMeters: Float) : LocationResult
    data object PermissionDenied : LocationResult
    data object ProviderDisabled : LocationResult
    data object Unavailable : LocationResult
}

interface LocationDataSource {
    suspend fun currentLocation(): LocationResult
}
