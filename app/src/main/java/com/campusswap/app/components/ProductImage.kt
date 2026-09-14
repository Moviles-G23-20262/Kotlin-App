package com.campusswap.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Category
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.LightBlue
import com.campusswap.app.ui.theme.SecondaryBlue

private val placeholderTints = listOf(
    LightBlue.copy(alpha = 0.55f),
    SecondaryBlue.copy(alpha = 0.35f),
    AccentBlue.copy(alpha = 0.16f),
)

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.ALL -> Icons.Outlined.Apps
    Category.CALCULATORS -> Icons.Outlined.Calculate
    Category.TEXTBOOKS -> Icons.Outlined.MenuBook
    Category.LAB_SUPPLIES -> Icons.Outlined.Science
    Category.SUPPLIES -> Icons.Outlined.Inventory2
    Category.NOTES -> Icons.Outlined.Description
    Category.ELECTRONICS -> Icons.Outlined.DevicesOther
}

/** Fixed aspect-ratio rounded placeholder standing in for real product photography (Section 2.3). */
@Composable
fun ProductPlaceholderImage(
    category: Category,
    seed: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
) {
    val tint = placeholderTints[seed.mod(placeholderTints.size)]
    Box(
        modifier = modifier
            .background(tint, RoundedCornerShape(cornerRadius.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(48.dp),
        )
    }
}
