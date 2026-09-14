package com.campusswap.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.CampusSwapChip
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.SectionHeader
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Category
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.ErrorRed

@Composable
fun HomeScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onSeeAllCategory: (Category) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onWishlistClick: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(Category.ALL) }

    val filtered: List<Product> = remember(selectedCategory, vm.allProducts.size) {
        if (selectedCategory == Category.ALL) vm.allProducts else vm.allProducts.filter { it.category == selectedCategory }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeHeader(
            unreadNotifications = vm.unreadNotificationCount,
            isDarkTheme = vm.isDarkTheme,
            onThemeToggle = vm::toggleTheme,
            onSearchClick = onSearchClick,
            onNotificationsClick = onNotificationsClick,
            onWishlistClick = onWishlistClick,
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                ) {
                    items(Category.entries.toList()) { category ->
                        CampusSwapChip(
                            label = category.label,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                        )
                    }
                }
            }

            item {
                Column {
                    SectionHeader(
                        title = "Featured for you",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onSeeAll = { onSeeAllCategory(selectedCategory) },
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    ) {
                        items(filtered.take(8)) { product ->
                            ProductCard(
                                product = product,
                                isWishlisted = vm.isWishlisted(product.id),
                                onClick = { onProductClick(product.id) },
                                onToggleWishlist = { vm.toggleWishlist(product.id) },
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Recently listed",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onSeeAll = { onSeeAllCategory(Category.ALL) },
                )
            }

            items(filtered.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { product ->
                        ProductCard(
                            product = product,
                            isWishlisted = vm.isWishlisted(product.id),
                            onClick = { onProductClick(product.id) },
                            onToggleWishlist = { vm.toggleWishlist(product.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
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
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "CampusSwap", style = MaterialTheme.typography.headlineMedium)
                Row {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.NightsStay,
                            contentDescription = "Toggle theme",
                        )
                    }
                    Box {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                        if (unreadNotifications > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 6.dp, end = 6.dp)
                                    .size(8.dp)
                                    .background(ErrorRed, androidx.compose.foundation.shape.CircleShape),
                            )
                        }
                    }
                    IconButton(onClick = onWishlistClick) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = "Wishlist")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onSearchClick)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = AccentBlue)
                Text(
                    text = "Search calculators, textbooks, courses...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
