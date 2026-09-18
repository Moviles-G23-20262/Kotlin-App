package com.campusswap.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.FakeSearchField
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.HeroCard
import com.campusswap.app.components.InitialsAvatar
import com.campusswap.app.components.MonoText
import com.campusswap.app.components.Pill
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.ProductRowCard
import com.campusswap.app.components.SectionHeader
import com.campusswap.app.components.SectionTitle
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.formatRating
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.FeaturedSeller
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onSeeAllCategory: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onWishlistClick: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    var selectedCategory by rememberSaveable { mutableStateOf(Category.ALL) }

    val offers: List<Product> = remember(selectedCategory, vm.allProducts.size) {
        val list = if (selectedCategory == Category.ALL) vm.allProducts.toList() else vm.allProducts.filter { it.category == selectedCategory }
        list.take(4)
    }
    val recommended: List<Product> = remember(vm.allProducts.size) {
        vm.allProducts.filter { it.seller.id != vm.currentUser.id }.drop(1).take(3)
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        HomeHeader(
            unreadNotifications = vm.unreadNotificationCount,
            isDarkTheme = vm.isDarkTheme,
            onThemeToggle = vm::toggleTheme,
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotificationsClick,
            onWishlistClick = onWishlistClick,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                HeroCard(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                    Column {
                        val chipShape = RoundedCornerShape(999.dp)
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(chipShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), chipShape)
                                .padding(horizontal = 12.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(CampusIcons.Sparkle, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            BodyText("Weekly Deal", color = Color.White.copy(alpha = 0.9f), weight = FontWeight.Medium)
                        }
                        HeadingText(
                            "Save big on course materials",
                            size = CampusType.sizeMd,
                            color = Color(0xFFF9F9F9),
                            lineHeight = CampusType.sizeMd * 1.2f,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        BodyText(
                            "Buy from fellow students and save up to 70% on textbooks, calculators and more.",
                            color = Color(0xFFF9F9F9).copy(alpha = 0.8f),
                            lineHeight = CampusType.sizeXs * 1.5f,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        val btnShape = RoundedCornerShape(8.dp)
                        Row(
                            modifier = Modifier
                                .clip(btnShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), btnShape)
                                .clickable(onClick = onSearchClick)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BodyText("Shop Now", color = Color(0xFFF9F9F9), weight = FontWeight.SemiBold)
                            Icon(CampusIcons.ArrowRight, contentDescription = null, tint = Color(0xFFF9F9F9), modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }

            // categorie
            item {
                Column {
                SectionTitle("Categories", modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
                ) {
                    items(Category.entries.toList()) { category ->
                        Pill(
                            label = category.label,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                        )
                    }
                }
                }
            }

            // offers
            item {
                Column {
                SectionHeader(
                    title = "Current Offers",
                    icon = CampusIcons.Flame,
                    onSeeAll = { onSeeAllCategory(selectedCategory) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                )
                if (offers.isEmpty()) {
                    BodyText(
                        "No offers in this category yet.",
                        color = c.textMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                    ) {
                        items(offers, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                modifier = Modifier.width(155.dp),
                                isWishlisted = vm.isWishlisted(product.id),
                                onClick = { onProductClick(product.id) },
                                onToggleWishlist = { vm.toggleWishlist(product.id) },
                                imageHeight = 110.dp,
                                showRating = false,
                            )
                        }
                    }
                }
                }
            }

            // Recomended
            item {
                SectionHeader(
                    title = "Recommended",
                    icon = CampusIcons.BookOpen,
                    onSeeAll = { onSeeAllCategory(Category.ALL) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                )
            }
            items(recommended, key = { "rec-${it.id}" }) { product ->
                RecommendedRow(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onAddToCart = { vm.addToCart(product) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }

            // Sellers
            item {
                Column {
                SectionTitle(
                    "Featured Sellers",
                    icon = CampusIcons.Star,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                ) {
                    items(vm.featuredSellers, key = { it.seller.id }) { featured ->
                        FeaturedSellerCard(featured)
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun RecommendedRow(
    product: Product,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CampusSwapTheme.colors
    ProductRowCard(product = product, onClick = onClick, modifier = modifier) {
        BodyText(
            product.title,
            color = c.text,
            weight = FontWeight.SemiBold,
            lineHeight = CampusType.sizeXs * 1.3f,
            maxLines = 2,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            Badge(product.condition.label)
            product.course?.let { Badge(it.code, highlighted = true) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PriceText(product.price, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            PrimaryButton(
                text = "Add to Cart",
                onClick = onAddToCart,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                fontSize = CampusType.sizeXs,
            )
        }
    }
}

@Composable
private fun FeaturedSellerCard(featured: FeaturedSeller) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .width(126.dp)
            .campusCard()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InitialsAvatar(featured.seller.name, size = 44.dp, modifier = Modifier.padding(bottom = 8.dp))
        BodyText(
            featured.seller.name,
            color = c.text,
            weight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(bottom = 2.dp),
        ) {
            Icon(CampusIcons.Star, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(13.dp))
            MonoText(featured.seller.rating?.let { formatRating(it) } ?: "New")
        }
        BodyText(
            "${featured.itemCount} item${if (featured.itemCount == 1) "" else "s"}",
            size = CampusType.size2xs,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeHeader(
    unreadNotifications: Int,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onWishlistClick: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    CampusHeader {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val logoShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(c.accent, logoShape)
                        .border(1.dp, c.accentLo, logoShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(CampusIcons.CampusSwap, contentDescription = null, tint = c.accentText, modifier = Modifier.size(18.dp))
                }
                HeadingText("Campus Swap", size = CampusType.sizeMd, letterSpacing = CampusType.sectionTitle.letterSpacing)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CampusIconButton(
                    icon = if (isDarkTheme) CampusIcons.Sun else CampusIcons.Moon,
                    contentDescription = "Toggle theme",
                    onClick = onThemeToggle,
                    iconSize = 18.dp,
                )
                Box {
                    CampusIconButton(CampusIcons.Bell, contentDescription = "Notifications", onClick = onNotificationsClick)
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(7.dp)
                                .background(c.accentHi, CircleShape),
                        )
                    }
                }
                CampusIconButton(CampusIcons.Heart, contentDescription = "Wishlist", onClick = onWishlistClick, iconTint = c.accent)
            }
        }
        FakeSearchField(placeholder = "Search course materials...", onClick = onSearchClick)
    }
}
