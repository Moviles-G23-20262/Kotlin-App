package com.campusswap.app.views.screens.cart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.campusswap.app.views.components.BackHeader
import com.campusswap.app.views.components.BodyText
import com.campusswap.app.views.components.BottomActionBar
import com.campusswap.app.views.components.CampusIcons
import com.campusswap.app.views.components.HeadingText
import com.campusswap.app.views.components.MonoText
import com.campusswap.app.views.components.OfflineBanner
import com.campusswap.app.views.components.PrimaryButton
import com.campusswap.app.views.components.SecondaryButton
import com.campusswap.app.views.components.StickyNote
import com.campusswap.app.views.components.campusCard
import com.campusswap.app.views.components.formatPrice
import com.campusswap.app.viewmodel.AppViewModel
import com.campusswap.app.model.Order
import com.campusswap.app.views.themes.CampusSwapTheme
import com.campusswap.app.views.themes.CampusType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun CheckoutScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (Int) -> Unit,
) {
    val c = CampusSwapTheme.colors
    var placing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "Checkout", onBack = onBack)

        OfflineBanner(
            visible = vm.needsConnectionToOrder,
            message = "You're offline. Your cart is saved: you can place the order when you're back online.",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Every exchange happens in person at a monitored campus point: the app never ships items or
            // takes payments, so the screen says so instead of offering options the order never used.
            CheckoutCard(title = "Meetup", icon = CampusIcons.MapPin) {
                CheckoutNote(
                    CampusIcons.Location,
                    "You'll agree on a safe, monitored campus point with the seller in the chat. " +
                        "The Campus Guardian suggests the best one for both of you.",
                )
            }

            CheckoutCard(title = "Payment", icon = CampusIcons.Wallet) {
                CheckoutNote(
                    CampusIcons.Wallet,
                    "Pay the seller in person when you meet, in cash, Nequi or Daviplata, after checking the item.",
                )
            }

            CheckoutCard(title = "Items", icon = CampusIcons.Package, titleBottomPadding = 10) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.cart.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BodyText(
                                line.product.title,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                            )
                            MonoText(
                                formatPrice(line.product.price),
                                size = CampusType.sizeXs,
                                color = c.accentHi,
                                weight = FontWeight.Medium,
                            )
                        }
                    }
                    if (vm.serviceFee > 0) SummaryRow("Service fee", formatPrice(vm.serviceFee))
                    TotalRow(vm.orderTotal)
                }
            }
        }

        vm.checkoutMessage?.let { message ->
            BodyText(message, color = c.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        BottomActionBar {
            PrimaryButton(
                text = when {
                    placing -> "Placing Order..."
                    vm.needsConnectionToOrder -> "Waiting for connection"
                    else -> "Place Order"
                },
                onClick = {
                    placing = true
                    scope.launch {
                        val number = vm.placeOrder()
                        placing = false
                        if (number != null) onOrderPlaced(number)
                    }
                },
                enabled = !placing && vm.cart.isNotEmpty() && !vm.needsConnectionToOrder,
                leadingIcon = CampusIcons.Send,
                contentPadding = PaddingValues(13.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CheckoutNote(icon: ImageVector, text: String) {
    val c = CampusSwapTheme.colors
    val noteShape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.elevated, noteShape)
            .border(1.dp, c.borderSubtle, noteShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(16.dp))
        BodyText(text, color = c.text2)
    }
}

@Composable
private fun CheckoutCard(
    title: String,
    icon: ImageVector,
    titleBottomPadding: Int = 12,
    content: @Composable () -> Unit,
) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(bottom = titleBottomPadding.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(15.dp))
            HeadingText(title, size = CampusType.sizeSm)
        }
        content()
    }
}

@Composable
fun OrderConfirmationScreen(
    orderNumber: String,
    /** The order just placed, so the confirmation can hand off to the exchange flow. */
    order: Order?,
    onBackToHome: () -> Unit,
    onKeepShopping: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteExchange: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val exchangeProduct = order?.products?.firstOrNull()
    // `animate-bounce-in`: 0.8 -> 1.04 -> 1.0 with a fade.
    val scale = remember { Animatable(0.8f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, keyframes { durationMillis = 350; 1f at 210 }) }
        scale.animateTo(1f, keyframes { durationMillis = 350; 1.04f at 210 })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .scale(scale.value)
                .alpha(fade.value)
                .size(96.dp)
                .shadow(14.dp, CircleShape, ambientColor = c.shadowAccent, spotColor = c.shadowAccent)
                .background(c.accent, CircleShape)
                .border(1.dp, c.accentLo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(CampusIcons.Check, contentDescription = null, tint = c.accentText, modifier = Modifier.size(44.dp))
        }
        HeadingText(
            "Order Placed",
            size = CampusType.sizeXl,
            weight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        BodyText("Your campus swap is confirmed.", color = c.textMuted, modifier = Modifier.padding(bottom = 28.dp))

        StickyNote(modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(CampusIcons.Inbox, contentDescription = null, tint = c.textMuted, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                Column {
                    BodyText("What happens next", color = c.text, weight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NextStep(CampusIcons.CheckCircle, "Seller has been notified")
                        NextStep(CampusIcons.Message, "1 · Agree on the meetup in chat")
                        NextStep(CampusIcons.MapPin, "2 · Meet at a monitored campus point")
                        NextStep(CampusIcons.Star, "3 · Check the item and rate each other")
                    }
                }
            }
        }

        MonoText(
            "Order #CSW-$orderNumber",
            size = CampusType.sizeXs,
            modifier = Modifier
                .padding(bottom = 28.dp)
                .campusCard()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )

        Column(
            modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (exchangeProduct != null) {
                PrimaryButton(
                    text = "Arrange the meetup",
                    onClick = { onOpenChat(exchangeProduct.id) },
                    leadingIcon = CampusIcons.Message,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Already met? Complete exchange",
                    onClick = { onCompleteExchange(exchangeProduct.id) },
                    leadingIcon = CampusIcons.CheckCircle,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Back to Home",
                    onClick = onBackToHome,
                    leadingIcon = CampusIcons.Home,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PrimaryButton(
                    text = "Back to Home",
                    onClick = onBackToHome,
                    leadingIcon = CampusIcons.Home,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = "Keep Shopping",
                    onClick = onKeepShopping,
                    leadingIcon = CampusIcons.ShoppingBag,
                    contentPadding = PaddingValues(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NextStep(icon: ImageVector, text: String) {
    val c = CampusSwapTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(15.dp))
        BodyText(text)
    }
}
