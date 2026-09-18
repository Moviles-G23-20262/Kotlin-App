package com.campusswap.app.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.BackHeader
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.BottomActionBar
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.MonoText
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductRowCard
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.formatPrice
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.CartLine
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

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
                        onIncrease = { vm.setQuantity(line.product.id, line.quantity + 1) },
                        onDecrease = { vm.setQuantity(line.product.id, line.quantity - 1) },
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

@Composable
private fun CartLineCard(
    line: CartLine,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
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
            PriceText(line.product.price * line.quantity, modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp))
            val stepperShape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .background(c.elevated, stepperShape)
                    .border(1.dp, c.borderSubtle, stepperShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    CampusIcons.Minus,
                    contentDescription = "Decrease quantity",
                    tint = c.text2,
                    modifier = Modifier.size(18.dp).plainClickable(onDecrease),
                )
                MonoText(
                    line.quantity.toString(),
                    size = CampusType.sizeSm,
                    color = c.text,
                    modifier = Modifier.defaultMinSize(minWidth = 16.dp),
                )
                Icon(
                    CampusIcons.Plus,
                    contentDescription = "Increase quantity",
                    tint = c.text2,
                    modifier = Modifier.size(18.dp).plainClickable(onIncrease),
                )
            }
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
