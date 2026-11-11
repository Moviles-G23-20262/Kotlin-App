package com.campusswap.app.views.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.views.components.CampusIconButton
import com.campusswap.app.views.components.BackHeader
import com.campusswap.app.views.components.Badge
import com.campusswap.app.views.components.BodyText
import com.campusswap.app.views.components.BottomActionBar
import com.campusswap.app.views.components.CampusIcons
import com.campusswap.app.views.components.EmptyState
import com.campusswap.app.views.components.OfflineBanner
import com.campusswap.app.views.components.HeadingText
import com.campusswap.app.views.components.PriceText
import com.campusswap.app.views.components.PrimaryButton
import com.campusswap.app.views.components.ProductRowCard
import com.campusswap.app.views.components.campusCard
import com.campusswap.app.views.components.formatPrice
import com.campusswap.app.viewmodel.AppViewModel
import com.campusswap.app.model.CartLine
import com.campusswap.app.views.themes.CampusSwapTheme
import com.campusswap.app.views.themes.CampusType

@Composable
fun CartScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onProductClick: (String) -> Unit,
    onCheckout: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    val lineCount = vm.cart.size

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(
            title = "Your Cart",
            onBack = onBack,
            subtitle = "($lineCount item${if (lineCount == 1) "" else "s"})",
        )

        OfflineBanner(
            visible = !vm.isOnline && vm.cart.isNotEmpty(),
            message = "You're offline. Your cart is saved on this phone; placing the order needs a connection.",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        if (vm.cart.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = CampusIcons.CartLight,
                    title = "Cart is empty",
                    message = "Browse items from fellow students.",
                    actionLabel = "Browse Items",
                    onAction = onBrowse,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(vm.cart, key = { it.product.id }) { line ->
                    CartLineCard(
                        line = line,
                        onClick = { onProductClick(line.product.id) },
                        onRemove = { vm.removeFromCart(line.product.id) },
                    )
                }
                item {
                    OrderSummaryCard(subtotal = vm.cartTotal, fee = vm.serviceFee, total = vm.orderTotal)
                }
            }

            BottomActionBar {
                PrimaryButton(
                    text = "Proceed to Checkout",
                    onClick = onCheckout,
                    trailingIcon = CampusIcons.ArrowRight,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * A listing is one physical item (one copy of a book, one calculator), so a line has no quantity:
 * the server reserves the listing once. The only action is removing it.
 */
@Composable
private fun CartLineCard(
    line: CartLine,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    ProductRowCard(product = line.product, onClick = onClick, thumbSize = 66.dp) {
        BodyText(
            line.product.title,
            color = c.text,
            weight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Badge(line.product.condition.label, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PriceText(line.product.price, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            CampusIconButton(
                CampusIcons.Close,
                contentDescription = "Remove ${line.product.title} from the cart",
                onClick = onRemove,
                iconSize = 16.dp,
            )
        }
    }
}

@Composable
private fun OrderSummaryCard(subtotal: Double, fee: Double, total: Double) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard()
            .padding(16.dp),
    ) {
        HeadingText("Order Summary", size = CampusType.sizeSm, modifier = Modifier.padding(bottom = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryRow("Subtotal", formatPrice(subtotal))
            SummaryRow("Service fee", formatPrice(fee))
            TotalRow(total)
        }
    }
}

@Composable
internal fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        BodyText(label)
        BodyText(value, textAlign = TextAlign.End)
    }
}

/** "Total" row with a top divider, shared by Cart and Checkout. */
@Composable
internal fun TotalRow(total: Double) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .drawBehind { drawLine(c.border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeadingText("Total", size = CampusType.sizeSm)
        PriceText(total)
    }
}
