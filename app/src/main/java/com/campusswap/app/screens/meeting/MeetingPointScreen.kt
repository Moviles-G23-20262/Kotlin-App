package com.campusswap.app.screens.meeting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.EmptyState
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.MeetingPoint
import com.campusswap.app.data.MeetingZoneType
import com.campusswap.app.data.SampleData
import com.campusswap.app.data.TimeSlot
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.JetBrainsMonoFamily
import com.campusswap.app.ui.theme.PrimaryBlue
import com.campusswap.app.ui.theme.SecondaryBlue
import com.campusswap.app.ui.theme.SuccessGreen
import com.campusswap.app.ui.theme.WarningAmber

// Approximate positions of both parties on the stylised map (normalised 0..1).
private val mePosition = Offset(0.13f, 0.17f)
private val otherPosition = Offset(0.84f, 0.72f)

/**
 * View 10 — Dynamic Safe Meeting Point (Campus Guardian CAS).
 * Suggests an equidistant, monitored campus zone during a shared free hour and lets the
 * buyer accept it or pick an alternative from a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingPointScreen(
    vm: AppViewModel,
    productId: String,
    onBack: () -> Unit,
    onProposed: () -> Unit,
) {
    val product = remember(productId, vm.allProducts.size) { vm.allProducts.find { it.id == productId } }
    if (product == null) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            title = "Listing not found",
            message = "This item may have been removed.",
            actionLabel = "Go back",
            onAction = onBack,
        )
        return
    }

    val recommended = remember { vm.recommendedMeetingPoint() }
    val existing = vm.meetingProposals[product.id]
    var selectedPoint by remember { mutableStateOf(existing?.point ?: recommended) }
    var selectedSlot by remember { mutableStateOf(existing?.slot ?: vm.recommendedTimeSlot()) }
    var showAlternatives by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val otherName = product.seller.name.substringBefore(' ')

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meeting point", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { showAlternatives = true },
                        modifier = Modifier.weight(1f).height(50.dp),
                    ) { Text("Change") }
                    Button(
                        onClick = {
                            vm.proposeMeeting(product, selectedPoint, selectedSlot)
                            onProposed()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Accept")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            CampusMap(
                selected = selectedPoint,
                otherName = otherName,
                onPointTap = { selectedPoint = it },
            )

            Column(modifier = Modifier.padding(16.dp)) {
                GuardianContextCard(slot = selectedSlot, otherName = otherName)

                Spacer(Modifier.height(14.dp))
                ProposalDetailCard(
                    point = selectedPoint,
                    isRecommended = selectedPoint.id == recommended.id,
                    otherName = otherName,
                )

                Text(
                    "Shared free hour",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SampleData.timeSlots.take(2).forEach { slot ->
                        SlotChip(slot, selectedSlot.id == slot.id, modifier = Modifier.weight(1f)) { selectedSlot = slot }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    SampleData.timeSlots.drop(2).forEach { slot ->
                        SlotChip(slot, selectedSlot.id == slot.id, modifier = Modifier.weight(1f)) { selectedSlot = slot }
                    }
                }

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                    Text(
                        "Only public, monitored campus zones are suggested. Payment always happens in person.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showAlternatives) {
        ModalBottomSheet(onDismissRequest = { showAlternatives = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                Text("Other safe zones nearby", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Ranked by combined walking time for you and $otherName.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                SampleData.meetingPoints
                    .sortedBy { it.walkMinutesMe + it.walkMinutesOther }
                    .forEach { point ->
                        AlternativeRow(
                            point = point,
                            selected = point.id == selectedPoint.id,
                            isRecommended = point.id == recommended.id,
                            otherName = otherName,
                            onClick = {
                                selectedPoint = point
                                showAlternatives = false
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
            }
        }
    }
}

/** Stylised campus map: walkways, building blocks, both parties and every pre-mapped safe zone. */
@Composable
private fun CampusMap(selected: MeetingPoint, otherName: String, onPointTap: (MeetingPoint) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val w = maxWidth
        val h = maxHeight
        val selectedOffset = Offset(selected.mapX, selected.mapY)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val block = PrimaryBlue.copy(alpha = 0.12f)
            val path = SecondaryBlue.copy(alpha = 0.55f)
            // Walkways
            drawLine(path, Offset(0f, size.height * 0.42f), Offset(size.width, size.height * 0.42f), strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(path, Offset(size.width * 0.50f, 0f), Offset(size.width * 0.50f, size.height), strokeWidth = 14f, cap = StrokeCap.Round)
            drawLine(path, Offset(size.width * 0.18f, size.height * 0.42f), Offset(size.width * 0.40f, size.height * 0.95f), strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(path, Offset(size.width * 0.50f, size.height * 0.68f), Offset(size.width, size.height * 0.68f), strokeWidth = 10f, cap = StrokeCap.Round)
            // Building blocks
            drawRoundRect(block, Offset(size.width * 0.06f, size.height * 0.08f), androidx.compose.ui.geometry.Size(size.width * 0.30f, size.height * 0.24f), androidx.compose.ui.geometry.CornerRadius(18f))
            drawRoundRect(block, Offset(size.width * 0.58f, size.height * 0.08f), androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.22f), androidx.compose.ui.geometry.CornerRadius(18f))
            drawRoundRect(block, Offset(size.width * 0.56f, size.height * 0.50f), androidx.compose.ui.geometry.Size(size.width * 0.36f, size.height * 0.12f), androidx.compose.ui.geometry.CornerRadius(18f))
            drawRoundRect(block, Offset(size.width * 0.06f, size.height * 0.56f), androidx.compose.ui.geometry.Size(size.width * 0.20f, size.height * 0.30f), androidx.compose.ui.geometry.CornerRadius(18f))
            // Green space
            drawCircle(SuccessGreen.copy(alpha = 0.16f), radius = size.width * 0.09f, center = Offset(size.width * 0.40f, size.height * 0.78f))
            // Routes from each party to the selected point
            val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
            val target = Offset(size.width * selectedOffset.x, size.height * selectedOffset.y)
            drawLine(AccentBlue, Offset(size.width * mePosition.x, size.height * mePosition.y), target, strokeWidth = 5f, pathEffect = dash, cap = StrokeCap.Round)
            drawLine(AccentBlue, Offset(size.width * otherPosition.x, size.height * otherPosition.y), target, strokeWidth = 5f, pathEffect = dash, cap = StrokeCap.Round)
            // Highlight ring on the selected safe zone
            drawCircle(AccentBlue.copy(alpha = 0.18f), radius = size.width * 0.11f, center = target)
        }

        // Other safe zones (small markers)
        SampleData.meetingPoints.filter { it.id != selected.id }.forEach { point ->
            SmallZoneMarker(
                point = point,
                modifier = Modifier
                    .offset(x = w * point.mapX - 14.dp, y = h * point.mapY - 14.dp)
                    .size(28.dp),
                onClick = { onPointTap(point) },
            )
        }

        // Selected zone (large pin with label)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = w * selected.mapX - 90.dp, y = h * selected.mapY - 46.dp).width(180.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                tonalElevation = 4.dp,
            ) {
                Text(
                    selected.name,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                )
            }
            Icon(Icons.Filled.Place, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(34.dp))
        }

        PersonMarker("You", mePosition, w, h, AccentBlue)
        PersonMarker(otherName, otherPosition, w, h, PrimaryBlue)

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Videocam, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Text("Monitored zones", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PersonMarker(label: String, position: Offset, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(x = w * position.x - 30.dp, y = h * position.y - 16.dp).width(60.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(top = 2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp), maxLines = 1)
        }
    }
}

@Composable
private fun SmallZoneMarker(point: MeetingPoint, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, if (point.isMonitored) SuccessGreen else WarningAmber),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(zoneIcon(point.zoneType), contentDescription = point.name, tint = PrimaryBlue, modifier = Modifier.size(15.dp))
        }
    }
}

/** Context card explaining what the CAS detected (shared break + micro-location). */
@Composable
private fun GuardianContextCard(slot: TimeSlot, otherName: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AccentBlue.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(36.dp).background(AccentBlue, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Campus Guardian suggestion", style = MaterialTheme.typography.titleSmall, color = AccentBlue)
                Text(
                    "You and $otherName are both free ${slot.day}, ${slot.label}. This spot is halfway between you and monitored by campus security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun ProposalDetailCard(point: MeetingPoint, isRecommended: Boolean, otherName: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.4f)),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).background(AccentBlue.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(zoneIcon(point.zoneType), contentDescription = null, tint = AccentBlue)
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(point.name, style = MaterialTheme.typography.headlineSmall)
                    Text(point.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isRecommended) {
                    Surface(shape = RoundedCornerShape(6.dp), color = SuccessGreen.copy(alpha = 0.14f), contentColor = SuccessGreen) {
                        Text("Best match", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WalkStat("You", point.walkMinutesMe, Modifier.weight(1f))
                WalkStat(otherName, point.walkMinutesOther, Modifier.weight(1f))
            }

            Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        if (point.isMonitored) Icons.Outlined.Videocam else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = if (point.isMonitored) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        if (point.isMonitored) "Monitored · verified safe zone" else "Public zone · not monitored",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (point.isMonitored) SuccessGreen else WarningAmber,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalkStat(who: String, minutes: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.DirectionsWalk, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    "$minutes min",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(who, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SlotChip(slot: TimeSlot, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) AccentBlue else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) AccentBlue else SecondaryBlue.copy(alpha = 0.5f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                Text(slot.label, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                if (slot.isSharedBreak) "${slot.day} · shared break" else slot.day,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun AlternativeRow(point: MeetingPoint, selected: Boolean, isRecommended: Boolean, otherName: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AccentBlue.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.dp, AccentBlue) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(zoneIcon(point.zoneType), contentDescription = null, tint = AccentBlue)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(point.name, style = MaterialTheme.typography.titleSmall)
                    if (isRecommended) {
                        Text("Best match", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                    }
                }
                Text(
                    "You ${point.walkMinutesMe} min · $otherName ${point.walkMinutesOther} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (point.isMonitored) Icons.Outlined.Videocam else Icons.Outlined.Shield,
                contentDescription = if (point.isMonitored) "Monitored" else "Not monitored",
                tint = if (point.isMonitored) SuccessGreen else WarningAmber,
                modifier = Modifier.size(18.dp),
            )
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = AccentBlue, modifier = Modifier.padding(start = 8.dp).size(18.dp))
            }
        }
    }
}

private fun zoneIcon(type: MeetingZoneType): ImageVector = when (type) {
    MeetingZoneType.LIBRARY -> Icons.Outlined.LocalLibrary
    MeetingZoneType.STUDENT_CENTER -> Icons.Outlined.Storefront
    MeetingZoneType.BUILDING_LOBBY -> Icons.Outlined.Apartment
    MeetingZoneType.PLAZA -> Icons.Outlined.Park
}
