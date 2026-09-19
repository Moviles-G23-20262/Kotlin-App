package com.campusswap.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.InitialsAvatar
import com.campusswap.app.components.MonoText
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductRowCard
import com.campusswap.app.components.SectionTitle
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.formatPrice
import com.campusswap.app.components.formatRating
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.Product
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

private data class BuyerMessage(val from: String, val product: Product, val message: String, val time: String)


@Composable
fun ProfileScreen(
    vm: AppViewModel,
    onProductClick: (String) -> Unit,
    onAddItem: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteExchange: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    val user = vm.currentUser
    val listings = vm.myListings
    val others = vm.allProducts.filter { it.seller.id != user.id }
    val purchases = vm.pendingExchanges
    val recentSales = others.take(2)
    val messages = others.drop(2).take(3).mapIndexed { index, product ->
        BuyerMessage(
            from = listOf("Maria Gomez", "Juan Restrepo", "Luisa Fernandez")[index % 3],
            product = product,
            message = listOf("Is this still available?", "Can we meet at the library?", "Does it include the case?")[index % 3],
            time = listOf("2m ago", "1h ago", "3h ago")[index % 3],
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        CampusHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(CampusIcons.Store, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(18.dp))
                HeadingText("My Seller Hub", size = CampusType.sizeMd, maxLines = 1, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Add Item",
                    onClick = onAddItem,
                    leadingIcon = CampusIcons.Plus,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    fontSize = CampusType.sizeXs,
                )
                CampusIconButton(CampusIcons.LogOut, contentDescription = "Log out", onClick = onLogout, iconSize = 18.dp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialsAvatar(user.name, size = 44.dp)
                        Column {
                            BodyText(user.name, color = c.text, weight = FontWeight.SemiBold, size = CampusType.sizeSm)
                            MonoText(user.maskedEmail + if (user.isVerified) " · Verified student" else "")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(CampusIcons.Package, listings.size.toString(), "Active Listings", Modifier.weight(1f))
                        StatCard(CampusIcons.Wallet, formatCompact(recentSales.sumOf { it.price }), "Earned This Month", Modifier.weight(1f))
                        StatCard(CampusIcons.TrendingUp, user.rating?.let { formatRating(it) } ?: "New", "Your Rating", Modifier.weight(1f))
                    }
                }
            }

            // every order stays here until the change is closed (View 12).
            item { SectionTitle("My Purchases", icon = CampusIcons.ShoppingBag, iconTint = c.text) }
            if (purchases.isEmpty()) {
                item {
                    BodyText(
                        "Nothing bought yet. Items you buy stay here until you meet the seller and rate the exchange.",
                        color = c.textMuted,
                    )
                }
            } else {
                items(purchases, key = { "buy-${it.product.id}" }) { purchase ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .campusCard()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                BodyText(
                                    purchase.product.title,
                                    color = c.text,
                                    weight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    modifier = Modifier.clickable { onProductClick(purchase.product.id) },
                                )
                                MonoText("Order #CSW-${purchase.order.number} · ${purchase.product.seller.name}")
                            }
                            PriceText(purchase.product.price)
                        }
                        val rating = purchase.rating
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                if (rating != null) CampusIcons.CheckCircle else if (purchase.isScheduled) CampusIcons.MapPin else CampusIcons.Clock,
                                contentDescription = null,
                                tint = if (rating != null) c.success else c.accentHi,
                                modifier = Modifier.size(14.dp),
                            )
                            BodyText(
                                when {
                                    rating != null -> "Closed · you rated ${rating.stars}.0"
                                    purchase.isScheduled -> "Meeting confirmed · ${purchase.proposal?.point?.name}"
                                    else -> "Waiting for a meeting point"
                                },
                                size = CampusType.size2xs,
                                color = c.textMuted,
                                modifier = Modifier.weight(1f),
                            )
                            if (rating == null) {
                                PrimaryButton(
                                    text = "Complete exchange",
                                    onClick = { onCompleteExchange(purchase.product.id) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = CampusType.sizeXs,
                                )
                            }
                        }
                    }
                }
            }

            // Active list
            item { SectionTitle("Active Listings", icon = CampusIcons.Package, iconTint = c.text) }
            if (listings.isEmpty()) {
                item {
                    BodyText("You have no active listings yet. Tap “Add Item” to publish one.", color = c.textMuted)
                }
            } else {
                items(listings, key = { "mine-${it.id}" }) { product ->
                    ProductRowCard(product = product, onClick = { onProductClick(product.id) }, thumbSize = 58.dp) {
                        BodyText(product.title, color = c.text, weight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.padding(bottom = 3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp),
                        ) {
                            Badge(product.condition.label)
                            Icon(CampusIcons.Eye, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(15.dp))
                            MonoText("${18 + product.imageSeed * 3} views")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PriceText(product.price)
                            val tagShape = RoundedCornerShape(6.dp)
                            BodyText(
                                "Active",
                                size = CampusType.size2xs,
                                color = c.success,
                                weight = FontWeight.Medium,
                                modifier = Modifier
                                    .background(c.elevated, tagShape)
                                    .border(1.dp, c.borderSubtle, tagShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // Recent sales
            item { SectionTitle("Recent Sales", icon = CampusIcons.TrendingUp, iconTint = c.text) }
            items(recentSales, key = { "sale-${it.id}" }) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .campusCard()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        BodyText(product.title, color = c.text, weight = FontWeight.SemiBold, maxLines = 1)
                        BodyText("Sold to Ana Cruz · 2 days ago", size = CampusType.size2xs, color = c.textMuted)
                    }
                    MonoText(formatPrice(product.price), size = CampusType.sizeXs, color = c.accentHi, weight = FontWeight.Medium)
                }
            }

            // Buyer messages
            item { SectionTitle("Buyer Messages", icon = CampusIcons.Message, iconTint = c.text) }
            items(messages, key = { "msg-${it.product.id}" }) { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .campusCard()
                        .clickable { onOpenChat(msg.product.id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InitialsAvatar(msg.from, size = 28.dp, fontSize = CampusType.size2xs)
                            BodyText(msg.from, color = c.text, weight = FontWeight.SemiBold)
                        }
                        MonoText(msg.time)
                    }
                    BodyText("Re: ${msg.product.title}", size = CampusType.size2xs, color = c.textMuted, maxLines = 1, modifier = Modifier.padding(bottom = 2.dp))
                    BodyText(msg.message)
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = modifier
            .campusCard()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = c.accentHi, modifier = Modifier.padding(bottom = 4.dp).size(16.dp))
        HeadingText(value, size = CampusType.sizeMd, maxLines = 1, textAlign = TextAlign.Center)
        BodyText(label, size = CampusType.size2xs, color = c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Short money label for the stat card, e.g. "$465k". */
private fun formatCompact(amount: Double): String = when {
    amount >= 1_000_000 -> "$" + String.format(java.util.Locale.US, "%.1fM", amount / 1_000_000)
    amount >= 1_000 -> "$" + (amount / 1_000).toInt() + "k"
    else -> "$" + amount.toInt()
}
