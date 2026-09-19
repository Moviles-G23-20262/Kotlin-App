package com.campusswap.app.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.InitialsAvatar
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.RatingLabel
import com.campusswap.app.components.SecondaryButton
import com.campusswap.app.components.SectionTitle
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.formatRating
import com.campusswap.app.components.pagerDot
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    vm: AppViewModel,
    productId: String,
    onBack: () -> Unit,
    onRelatedClick: (String) -> Unit,
    onChatWithSeller: (String) -> Unit,
    onBuyNow: () -> Unit,
    onCompleteExchange: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val product = remember(productId, vm.allProducts.size) { vm.allProducts.find { it.id == productId } }

    if (product == null) {
        Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = CampusIcons.Package,
                title = "Listing not found",
                message = "This item may have been removed.",
                actionLabel = "Go back",
                onAction = onBack,
            )
        }
        return
    }

    val related = remember(product.id, vm.allProducts.size) {
        vm.allProducts.filter { it.id != product.id && it.category == product.category }.take(3)
    }
    var addedTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(addedTick) {
        if (addedTick > 0) {
            delay(1500)
            addedTick = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        // Image carousel
        Box(modifier = Modifier.fillMaxWidth().height(290.dp).background(c.elevated)) {
            val pagerState = rememberPagerState(pageCount = { 3 })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ProductPlaceholderImage(
                    category = product.category,
                    seed = product.imageSeed + page,
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 0,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.4f to Color.Transparent)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CampusIconButton(CampusIcons.Back, contentDescription = "Back", onClick = onBack)
                val wishlisted = vm.isWishlisted(product.id)
                CampusIconButton(
                    icon = if (wishlisted) CampusIcons.HeartFilled else CampusIcons.Heart,
                    contentDescription = "Toggle wishlist",
                    onClick = { vm.toggleWishlist(product.id) },
                    iconTint = c.accent,
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(3) { index ->
                    Box(Modifier.pagerDot(active = index == pagerState.currentPage, activeColor = c.accentHi))
                }
            }
        }

        // Details sheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
                .background(c.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Badge(product.category.label, highlighted = true)
                product.course?.let { Badge(it.code) }
                Badge(product.condition.label)
            }
            HeadingText(
                product.title,
                size = CampusType.sizeMd,
                lineHeight = CampusType.sizeMd * 1.25f,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier.padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PriceText(product.price, size = CampusType.sizeLg, weight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                RatingLabel(
                    product.rating?.let { "${formatRating(it)} (${product.reviewCount} reviews)" } ?: "No reviews yet",
                    size = CampusType.sizeXs,
                )
            }

            SellerCard(product = product, onChat = { onChatWithSeller(product.id) })

            HeadingText("About this item", size = CampusType.sizeSm, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            BodyText(product.description, lineHeight = CampusType.sizeXs * 1.7f)

            // Already bought and not rated yet: the listing points straight at View 12.
            if (vm.hasPendingExchange(product.id)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .campusCard(borderColor = c.accentLo)
                        .plainClickable { onCompleteExchange(product.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(CampusIcons.Package, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        BodyText("You bought this item", color = c.text, weight = FontWeight.SemiBold)
                        BodyText(
                            "Check it when you meet and rate ${product.seller.name.substringBefore(' ')}.",
                            size = CampusType.size2xs,
                            color = c.textMuted,
                        )
                    }
                    BodyText("Open", color = c.accentHi, weight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    text = if (addedTick > 0) "Added!" else "Add to Cart",
                    onClick = {
                        vm.addToCart(product)
                        addedTick++
                    },
                    leadingIcon = CampusIcons.Cart,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Buy Now",
                    onClick = {
                        vm.addToCart(product)
                        onBuyNow()
                    },
                    trailingIcon = CampusIcons.ArrowRight,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    modifier = Modifier.weight(1f),
                )
            }

            if (related.isNotEmpty()) {
                SectionTitle("Related Materials", modifier = Modifier.padding(bottom = 12.dp))
            }
        }

        if (related.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().offset(y = (-16).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            ) {
                items(related, key = { it.id }) { item ->
                    RelatedCard(item, onClick = { onRelatedClick(item.id) })
                }
            }
        }
    }
}

@Composable
private fun SellerCard(product: Product, onChat: () -> Unit) {
    val c = CampusSwapTheme.colors
    val seller = product.seller
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.elevated, shape)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsAvatar(seller.name, size = 42.dp, background = c.surface)
        Column(modifier = Modifier.weight(1f)) {
            BodyText(seller.name, color = c.text, weight = FontWeight.SemiBold)
            val rating = seller.rating?.let { formatRating(it) } ?: "New"
            val verified = if (seller.isVerified) " · Verified student" else ""
            RatingLabel("$rating · ${seller.reviewCount} reviews$verified", modifier = Modifier.padding(top = 4.dp))
        }
        val btnShape = RoundedCornerShape(10.dp)
        Row(
            modifier = Modifier
                .background(c.elevated, btnShape)
                .border(1.dp, c.borderSubtle, btnShape)
                .clickable(onClick = onChat)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(CampusIcons.Message, contentDescription = null, tint = c.text2, modifier = Modifier.size(15.dp))
            BodyText("Chat", color = c.text2)
        }
    }
}

@Composable
private fun RelatedCard(product: Product, onClick: () -> Unit) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .width(135.dp)
            .campusCard(radius = 14.dp)
            .clickable(onClick = onClick),
    ) {
        ProductPlaceholderImage(
            category = product.category,
            seed = product.imageSeed,
            modifier = Modifier.fillMaxWidth().height(85.dp),
            cornerRadius = 0,
        )
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            BodyText(
                product.title,
                size = CampusType.size2xs,
                color = c.text,
                weight = FontWeight.SemiBold,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 3.dp),
            )
            PriceText(product.price, size = CampusType.sizeXs)
        }
    }
}
