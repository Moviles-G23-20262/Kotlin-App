package com.campusswap.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.BackHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.ProductCard
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.ui.theme.CampusSwapTheme

/**
 * Saved items. The bell in the header opens the alerts screen (View 11),
 * which is where saved searches and their matches live.
 */
@Composable
fun WishlistScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onOpenAlerts: () -> Unit,
    onBack: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    val saved = vm.allProducts.filter { vm.isWishlisted(it.id) }
    val matchCount = vm.alertMatches.size

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(
            title = "Wishlist",
            onBack = onBack,
            subtitle = "(${saved.size} saved)",
        ) {
            Box {
                CampusIconButton(
                    icon = CampusIcons.Bell,
                    contentDescription = "Alerts",
                    onClick = onOpenAlerts,
                    iconSize = 18.dp,
                )
                if (matchCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(7.dp)
                            .background(c.accentHi, CircleShape),
                    )
                }
            }
        }

        if (saved.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = CampusIcons.Heart,
                    title = "Your wishlist is empty",
                    message = "Tap the heart on any listing to save it here.",
                    actionLabel = "Set up an alert instead",
                    onAction = onOpenAlerts,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(saved, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        isWishlisted = true,
                        onClick = { onProductClick(product.id) },
                        onToggleWishlist = { vm.toggleWishlist(product.id) },
                    )
                }
            }
        }
    }
}
