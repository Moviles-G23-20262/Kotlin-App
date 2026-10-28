package com.campusswap.app.model.location

import com.campusswap.app.model.domain.GeoPoint

sealed interface LocationResult {
    data class Fix(val point: GeoPoint, val accuracyMeters: Float) : LocationResult
    data object PermissionDenied : LocationResult
    data object ProviderDisabled : LocationResult
    data object Unavailable : LocationResult
}

interface LocationDataSource {
    suspend fun currentLocation(): LocationResult
}
