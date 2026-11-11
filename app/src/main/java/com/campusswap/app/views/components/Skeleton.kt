package com.campusswap.app.views.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campusswap.app.views.themes.CampusSwapTheme

/**
 * A pulsing placeholder with the shape of the content that is still loading, so the first load reads
 * as "loading" instead of an empty list ("No offers in this category yet.").
 */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    val c = CampusSwapTheme.colors
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Box(
        modifier
            .semantics { contentDescription = "Loading" }
            .alpha(pulse)
            .background(c.elevated, RoundedCornerShape(radius)),
    )
}
