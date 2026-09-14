package com.campusswap.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.ErrorRed

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    isWishlisted: Boolean = false,
    onClick: () -> Unit = {},
    onToggleWishlist: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .width(168.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Box {
                ProductPlaceholderImage(
                    category = product.category,
                    seed = product.imageSeed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = 0,
                )
                if (onToggleWishlist != null) {
                    IconButton(
                        onClick = onToggleWishlist,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle wishlist",
                            tint = if (isWishlisted) ErrorRed else MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatPrice(product.price),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                ConditionBadge(product.condition)
                RatingStars(product.rating, product.reviewCount, starSize = 12.dp)
            }
        }
    }
}
