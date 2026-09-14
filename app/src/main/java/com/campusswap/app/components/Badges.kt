package com.campusswap.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Condition
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.SecondaryBlue
import com.campusswap.app.ui.theme.SuccessGreen
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun ConditionBadge(condition: Condition, modifier: Modifier = Modifier) {
    val color = when (condition) {
        Condition.LIKE_NEW -> SuccessGreen
        Condition.GOOD -> AccentBlue
        Condition.FAIR -> SecondaryBlue
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = condition.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = "Verified student",
            tint = AccentBlue,
            modifier = Modifier,
        )
        if (!compact) {
            Text("Verified student", style = MaterialTheme.typography.labelMedium, color = AccentBlue)
        }
    }
}

@Composable
fun RatingStars(
    rating: Double?,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    starSize: androidx.compose.ui.unit.Dp = 14.dp,
) {
    if (rating == null) {
        Text(
            text = "No reviews yet",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val fullStars = floor(rating).toInt()
        val hasHalf = (rating - fullStars) >= 0.5
        repeat(5) { index ->
            val icon = when {
                index < fullStars -> Icons.Filled.Star
                index == fullStars && hasHalf -> Icons.Filled.StarHalf
                else -> Icons.Outlined.Star
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.padding(end = 1.dp).size(starSize),
            )
        }
        Text(
            text = " ${formatRating(rating)} ($reviewCount)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatRating(rating: Double): String {
    val rounded = (rating * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}.0" else rounded.toString()
}
