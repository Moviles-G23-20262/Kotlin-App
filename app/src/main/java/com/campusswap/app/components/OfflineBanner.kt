package com.campusswap.app.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ErrorOutline
import com.campusswap.app.data.PendingSync
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.ui.theme.CampusSwapTheme
import java.time.Duration

/** Amber strip that explains what the screen is doing while there's no connection. */
@Composable
fun OfflineBanner(visible: Boolean, message: String, modifier: Modifier = Modifier) {
    val c = CampusSwapTheme.colors
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.warning.copy(alpha = 0.14f), shape)
                .border(1.dp, c.warning.copy(alpha = 0.45f), shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
            BodyText(message, color = c.text, weight = FontWeight.Medium)
        }
    }
}

/** "just now", "5 min ago", "3 h ago", "2 days ago". */
fun timeAgo(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val elapsed = Duration.ofMillis((nowMillis - fromMillis).coerceAtLeast(0))
    return when {
        elapsed.toMinutes() < 1 -> "just now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()} min ago"
        elapsed.toDays() < 1 -> "${elapsed.toHours()} h ago"
        elapsed.toDays() == 1L -> "1 day ago"
        else -> "${elapsed.toDays()} days ago"
    }
}

/** Shown on a listing that only exists on this phone so far. */
@Composable
fun PendingSyncLabel(sync: PendingSync, modifier: Modifier = Modifier) {
    val c = CampusSwapTheme.colors
    val failed = sync.failedReason != null
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            if (failed) Icons.Outlined.ErrorOutline else Icons.Outlined.CloudUpload,
            contentDescription = null,
            tint = if (failed) c.error else c.warning,
            modifier = Modifier.size(14.dp),
        )
        BodyText(
            if (failed) "Not published: ${sync.failedReason}" else "Waiting to publish",
            color = if (failed) c.error else c.warning,
            weight = FontWeight.Medium,
            maxLines = 2,
        )
    }
}
