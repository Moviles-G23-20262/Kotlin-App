package com.campusswap.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.ProductCard
import com.campusswap.app.components.RatingStars
import com.campusswap.app.components.SectionHeader
import com.campusswap.app.components.VerifiedBadge
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.ui.theme.AccentBlue

@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val listings = vm.myListings
    val user = vm.currentUser

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(96.dp).background(AccentBlue.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(48.dp))
                }
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = user.maskedEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (user.isVerified) {
                    VerifiedBadge(modifier = Modifier.padding(top = 8.dp))
                }
                RatingStars(
                    rating = user.rating,
                    reviewCount = user.reviewCount,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item { HorizontalDivider() }

        item {
            SectionHeader(title = "My listings (${listings.size})")
        }

        if (listings.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "No listings yet",
                    message = "Items you publish from the Sell tab will appear here.",
                )
            }
        } else {
            items(listings.chunked(2)) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { product ->
                        ProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Text("  Log out")
            }
        }
    }
}
