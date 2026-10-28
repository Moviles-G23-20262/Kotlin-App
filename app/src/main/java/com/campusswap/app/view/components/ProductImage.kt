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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Category
import com.campusswap.app.ui.theme.CampusSwapTheme

fun categoryIcon(category: Category): ImageVector = when (category) {
    Category.ALL -> Icons.Outlined.Apps
    Category.CALCULATORS -> Icons.Outlined.Calculate
    Category.TEXTBOOKS -> Icons.Outlined.MenuBook
    Category.LAB_SUPPLIES -> Icons.Outlined.Science
    Category.SUPPLIES -> Icons.Outlined.Inventory2
    Category.NOTES -> Icons.Outlined.Description
    Category.ELECTRONICS -> Icons.Outlined.DevicesOther
}

/**
 * Stand-in for product photography: an image box on `--bg-elevated` (like the prototype's
 * `<img>` containers) with a soft accent gradient and the category glyph.
 */
@Composable
fun ProductPlaceholderImage(
    category: Category,
    seed: Int,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
) {
    val c = CampusSwapTheme.colors
    val alphas = listOf(0.55f, 0.35f, 0.75f)
    val tint = c.accentLo.copy(alpha = alphas[seed.mod(alphas.size)])
    var minSidePx by remember { mutableStateOf(0) }
    val iconSize = with(LocalDensity.current) { (minSidePx * 0.36f).toDp() }.coerceIn(20.dp, 72.dp)
    val shape = RoundedCornerShape(cornerRadius.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(c.elevated)
            .background(Brush.linearGradient(listOf(tint, c.elevated)))
            .onSizeChanged { minSidePx = minOf(it.width, it.height) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = c.accentHi.copy(alpha = 0.85f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun ProductPlaceholderFill(category: Category, seed: Int) {
    ProductPlaceholderImage(category = category, seed = seed, modifier = Modifier.fillMaxSize(), cornerRadius = 0)
}