package com.campusswap.app.screens.rating

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.SecondaryButton
import com.campusswap.app.components.plainClickable
import com.campusswap.app.domain.Proximity
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private val RADIUS_METERS = Proximity.CONFIRMATION_RADIUS_METERS.toInt()

@Composable
fun ExchangeCheckIn(viewModel: ExchangeCheckInViewModel, pointName: String?) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    val place = pointName ?: "the meeting point"

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.checkIn() else viewModel.onPermissionDenied()
    }

    val requestCheckIn = {
        when {
            hasPreciseLocation(context) -> viewModel.checkIn()
            context.shouldShowLocationRationale() -> showRationale = true
            else -> permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    when (val s = state) {
        CheckInState.Idle -> {
            Hint("We'll check once that you're within $RADIUS_METERS m of $place. Your location is only sent with this exchange.")
            PrimaryButton(
                text = "I'm here — confirm with GPS",
                onClick = requestCheckIn,
                leadingIcon = CampusIcons.MapPin,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        CheckInState.Locating -> Progress("Getting your location…")
        CheckInState.Sending -> Progress("Confirming the exchange…")
        is CheckInState.TooFar -> {
            Hint("You're about ${s.distanceMeters} m from $place. Get within $RADIUS_METERS m and try again.", error = true)
            PrimaryButton(
                text = "Try again",
                onClick = requestCheckIn,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        is CheckInState.Manual -> ManualFallback(
            reason = s.reason,
            onConfirmManually = viewModel::confirmManually,
            onRetryGps = requestCheckIn,
            onOpenSettings = { context.openSettingsFor(s.reason) },
        )
        is CheckInState.Confirmed -> Confirmed(s.verifiedByGps)
        is CheckInState.Failed -> {
            Hint(failureMessage(s.reason), error = true)
            if (s.reason != FailReason.NOT_SYNCED) {
                PrimaryButton(
                    text = "Retry",
                    onClick = viewModel::retry,
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false; viewModel.onPermissionDenied() },
            title = { Text("Use your location once?") },
            text = {
                Text(
                    "Campus Swap checks a single time that you're at $place, so both of you know the exchange " +
                        "happened where you agreed. It never tracks you in the background.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showRationale = false; permissionLauncher.launch(LOCATION_PERMISSIONS) }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false; viewModel.onPermissionDenied() }) { Text("Not now") }
            },
        )
    }
}

@Composable
private fun ManualFallback(
    reason: ManualReason,
    onConfirmManually: () -> Unit,
    onRetryGps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    Hint(manualMessage(reason))
    val settingsLabel = when (reason) {
        ManualReason.PERMISSION_DENIED -> "Open app settings"
        ManualReason.LOCATION_OFF -> "Turn on location"
        else -> null
    }
    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (settingsLabel != null) {
            BodyText(settingsLabel, size = CampusType.size2xs, color = c.accentHi, weight = FontWeight.SemiBold, modifier = Modifier.plainClickable(onOpenSettings))
        }
        if (reason != ManualReason.POINT_NOT_MAPPED) {
            BodyText("Try GPS again", size = CampusType.size2xs, color = c.accentHi, weight = FontWeight.SemiBold, modifier = Modifier.plainClickable(onRetryGps))
        }
    }
    SecondaryButton(
        text = "I'm at the meeting point",
        onClick = onConfirmManually,
        contentPadding = PaddingValues(12.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}

@Composable
private fun Confirmed(verifiedByGps: Boolean?) {
    val c = CampusSwapTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(CampusIcons.Check, contentDescription = null, tint = c.success, modifier = Modifier.size(18.dp))
        BodyText(
            when (verifiedByGps) {
                true -> "Exchange confirmed · verified by GPS"
                false -> "Exchange confirmed manually"
                null -> "Exchange confirmed"
            },
            color = c.success,
            weight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun Progress(text: String) {
    val c = CampusSwapTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = c.accentHi, strokeWidth = 2.dp)
        BodyText(text, color = c.text2)
    }
}

@Composable
private fun Hint(text: String, error: Boolean = false) {
    val c = CampusSwapTheme.colors
    BodyText(text, size = CampusType.size2xs, color = if (error) c.error else c.textMuted)
}

private fun manualMessage(reason: ManualReason): String = when (reason) {
    ManualReason.PERMISSION_DENIED -> "Precise location is off for Campus Swap, so we can't verify the spot automatically."
    ManualReason.LOCATION_OFF -> "Location services are turned off on this phone."
    ManualReason.NO_FIX -> "We couldn't get a GPS fix. Step outside or near a window, or confirm by hand."
    ManualReason.POINT_NOT_MAPPED -> "This meeting point has no GPS reference yet, so confirm by hand."
}

private fun failureMessage(reason: FailReason): String = when (reason) {
    FailReason.OFFLINE -> "No connection. Your confirmation wasn't sent yet."
    FailReason.REJECTED -> "The server couldn't record this exchange. Try again in a moment."
    FailReason.NOT_SYNCED -> "This listing isn't on the server yet, so the exchange can't be recorded."
}

private fun hasPreciseLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Context.shouldShowLocationRationale(): Boolean =
    findActivity()?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION) } == true

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.openSettingsFor(reason: ManualReason) {
    val intent = when (reason) {
        ManualReason.LOCATION_OFF -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    startActivity(intent)
}
