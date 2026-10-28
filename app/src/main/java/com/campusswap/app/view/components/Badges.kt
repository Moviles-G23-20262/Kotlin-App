package com.campusswap.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Condition
import com.campusswap.app.ui.theme.CampusSwapTheme
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun ConditionBadge(condition: Condition, modifier: Modifier = Modifier) {
    Badge(text = condition.label, modifier = modifier)
}

@Composable
fun VerifiedBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.VerifiedUser,
            contentDescription = "Verified student",
            tint = c.accentHi,
            modifier = Modifier.size(14.dp),
        )
        if (!compact) {
            BodyText("Verified student", color = c.accentHi, weight = FontWeight.Medium)
        }
    }
}

@Composable
fun RatingStars(
    rating: Double?,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 13.dp,
) {
    val c = CampusSwapTheme.colors
    if (rating == null) {
        BodyText("No reviews yet", modifier = modifier, color = c.textMuted)
        return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        val fullStars = floor(rating).toInt()
        val hasHalf = (rating - fullStars) >= 0.5
        repeat(5) { index ->
            val filled = index < fullStars || (index == fullStars && hasHalf)
            Icon(
                imageVector = if (filled) CampusIcons.Star else CampusIcons.StarOutline,
                contentDescription = null,
                tint = c.accentHi,
                modifier = Modifier.padding(end = 1.dp).size(starSize),
            )
        }
        MonoText(" ${formatRating(rating)} ($reviewCount)")
    }
}

fun formatRating(rating: Double): String {
    val rounded = (rating * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}.0" else rounded.toString()
}
