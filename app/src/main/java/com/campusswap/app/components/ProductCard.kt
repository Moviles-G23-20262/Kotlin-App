package com.campusswap.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    isWishlisted: Boolean = false,
    onClick: () -> Unit = {},
    onToggleWishlist: (() -> Unit)? = null,
    imageHeight: Dp = 120.dp,
    showRating: Boolean = true,
) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = modifier
            .campusCard(radius = 14.dp)
            .clickable(onClick = onClick),
    ) {
        ProductPlaceholderImage(
            category = product.category,
            seed = product.imageSeed,
            modifier = Modifier.fillMaxWidth().height(imageHeight),
            cornerRadius = 0,
        )
        Column(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                BodyText(
                    text = product.title,
                    color = c.text,
                    weight = FontWeight.SemiBold,
                    lineHeight = CampusType.sizeXs * 1.3f,
                    maxLines = 2,
                    minLines = 2,
                    modifier = Modifier.weight(1f).padding(end = 4.dp, bottom = 4.dp),
                )
                if (onToggleWishlist != null) {
                    Icon(
                        imageVector = if (isWishlisted) CampusIcons.HeartFilled else CampusIcons.Heart,
                        contentDescription = "Toggle wishlist",
                        tint = c.accent,
                        modifier = Modifier
                            .size(20.dp)
                            .plainClickable(onToggleWishlist),
                    )
                }
            }
            if (showRating) {
                val ratingText = product.rating?.let { "${formatRating(it)} (${product.reviewCount})" } ?: "New"
                RatingLabel(ratingText, modifier = Modifier.padding(bottom = 8.dp))
            }
            Badge(product.condition.label, modifier = Modifier.padding(bottom = 6.dp))
            PriceText(product.price)
        }
    }
}

/** Horizontal list item used by "Recommended" and the cart: thumbnail + details. */
@Composable
fun ProductRowCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbSize: Dp = 68.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .campusCard()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductPlaceholderImage(
            category = product.category,
            seed = product.imageSeed,
            modifier = Modifier.size(thumbSize),
            cornerRadius = 10,
        )
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
