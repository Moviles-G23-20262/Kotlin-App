package com.campusswap.app.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.CampusSwapChip
import com.campusswap.app.components.ConditionBadge
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.RatingStars
import com.campusswap.app.components.VerifiedBadge
import com.campusswap.app.components.formatPrice
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.DeepNavy

@Composable
fun ProductDetailScreen(
    vm: AppViewModel,
    productId: String,
    onBack: () -> Unit,
    onRelatedClick: (String) -> Unit,
    onChatWithSeller: (String) -> Unit,
) {
    val product = remember(productId, vm.allProducts.size) { vm.allProducts.find { it.id == productId } }

    if (product == null) {
        EmptyState(
            icon = Icons.Filled.ArrowBack,
            title = "Listing not found",
            message = "This item may have been removed.",
            actionLabel = "Go back",
            onAction = onBack,
        )
        return
    }

    val related = remember(product.id, vm.allProducts.size) {
        vm.allProducts.filter { it.id != product.id && (it.category == product.category || it.course?.code == product.course?.code) }.take(6)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column {
                Box {
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
                        ProductPlaceholderImage(
                            category = product.category,
                            seed = product.imageSeed + page,
                            modifier = Modifier.fillMaxSize(),
                            cornerRadius = 0,
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        if (index == pagerState.currentPage) AccentBlue else Color.White.copy(alpha = 0.6f),
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(DeepNavy.copy(alpha = 0.35f), CircleShape),
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    IconButton(
                        onClick = { vm.toggleWishlist(product.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(DeepNavy.copy(alpha = 0.35f), CircleShape),
                    ) {
                        val wishlisted = vm.isWishlisted(product.id)
                        Icon(
                            imageVector = if (wishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle wishlist",
                            tint = Color.White,
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CampusSwapChip(label = product.category.label, selected = false, onClick = {})
                        if (product.course != null) {
                            CampusSwapChip(label = product.course.code, selected = false, onClick = {})
                        }
                    }
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatPrice(product.price),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        ConditionBadge(product.condition)
                    }
                    RatingStars(product.rating, product.reviewCount, modifier = Modifier.padding(top = 8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).background(AccentBlue.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentBlue)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(product.seller.name, style = MaterialTheme.typography.titleSmall)
                                if (product.seller.isVerified) {
                                    VerifiedBadge(compact = true)
                                }
                            }
                            RatingStars(product.seller.rating, product.seller.reviewCount)
                        }
                    }

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )

                    if (related.isNotEmpty()) {
                        Text(
                            text = "Related materials",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }

                if (related.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    ) {
                        items(related) { relatedProduct ->
                            ProductCard(
                                product = relatedProduct,
                                isWishlisted = vm.isWishlisted(relatedProduct.id),
                                onClick = { onRelatedClick(relatedProduct.id) },
                                onToggleWishlist = { vm.toggleWishlist(relatedProduct.id) },
                            )
                        }
                    }
                }
            }
        }

        Surface(tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { vm.addToCart(product) },
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text("Add to Cart")
                }
                Button(
                    onClick = { onChatWithSeller(product.id) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Chat with Seller", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}
