package com.campusswap.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.campusswap.app.domain.GeoPoint
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * One-shot fix from Google Play services. A single getCurrentLocation call is enough to check in at a
 * meeting point, so there is no continuous listener draining the battery and no background permission.
 */
class FusedLocationDataSource(private val context: Context) : LocationDataSource {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission") // Checked by hasPrecisePermission() just before the call.
    override suspend fun currentLocation(): LocationResult {
        if (!hasPrecisePermission()) return LocationResult.PermissionDenied
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) return LocationResult.ProviderDisabled

        // Cancelling the token stops Play services from keeping the GPS on after a timeout.
        val cancellation = CancellationTokenSource()
        val location = try {
            withTimeoutOrNull(FIX_TIMEOUT_MS) {
                client.getCurrentLocation(FRESH_FIX_REQUEST, cancellation.token).await()
            }
        } finally {
            cancellation.cancel()
        } ?: return LocationResult.Unavailable

        return LocationResult.Fix(GeoPoint(location.latitude, location.longitude), location.accuracy)
    }

    private fun hasPrecisePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val FIX_TIMEOUT_MS = 15_000L

        // By default Play services may answer with a cached fix up to a minute old, so a user who walks
        // to the point and retries would still be "too far" (or could walk away and still confirm).
        val FRESH_FIX_REQUEST: CurrentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()
    }
}
