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
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.MeetingPoint
import com.campusswap.app.model.TimeSlot
import com.campusswap.app.model.orders.PaymentMethod
import com.campusswap.app.viewmodel.CheckoutUiState
import com.campusswap.app.viewmodel.CheckoutViewModel
import com.campusswap.app.views.components.Pill
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val checkout: CheckoutViewModel = viewModel(factory = CheckoutViewModel.factory(container))
    val state by checkout.state.collectAsState()
    val productIds = vm.cart.map { it.product.id }
    val ready = state.isComplete(productIds)
    val missingItems = state.incompleteCount(productIds)
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
            vm.cart.forEach { line ->
                val product = line.product
                MeetupCard(
                    title = "${product.title} · ${product.seller.name.substringBefore(' ')}",
                    state = state,
                    productId = product.id,
                    onPoint = { checkout.choosePoint(product.id, it) },
                    onDay = { checkout.chooseDay(product.id, it) },
                    onSlot = { checkout.chooseSlot(product.id, it) },
                    onPayment = { checkout.choosePayment(product.id, it) },
                )
            }

            CheckoutNote(CampusIcons.Wallet, "You pay each seller in person when you meet, after checking the item.")

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
                    !ready -> if (missingItems == 1) "Complete 1 more item" else "Complete $missingItems more items"
                    else -> "Place Order"
                },
                onClick = {
                    placing = true
                    scope.launch {
                        val number = vm.placeOrder()
                        placing = false
                        if (number != null) {
                            vm.lastPlacedOrder?.let { vm.arrangeMeetups(container.meetupArranger, it, state.plans()) }
                            onOrderPlaced(number)
                        }
                    }
                },
                enabled = !placing && ready && vm.cart.isNotEmpty() && !vm.needsConnectionToOrder,
                leadingIcon = CampusIcons.Send,
                contentPadding = PaddingValues(13.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MeetupCard(
    title: String,
    state: CheckoutUiState,
    productId: String,
    onPoint: (MeetingPoint) -> Unit,
    onDay: (Int) -> Unit,
    onSlot: (TimeSlot) -> Unit,
    onPayment: (PaymentMethod) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val choice = state.choiceFor(productId)
    val taken = state.takenByOthers(productId)
    CheckoutCard(title = title, icon = CampusIcons.MapPin) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyText("Where", color = c.textMuted)
            if (state.points.isEmpty()) {
                BodyText("Loading campus points...", color = c.textMuted)
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.points.forEach { point ->
                        Pill(point.name, selected = choice.point?.id == point.id, onClick = { onPoint(point) }, small = true)
                    }
                }
                choice.point?.let { point ->
                    BodyText(
                        if (point.isMonitored) "Monitored point" else "Not monitored: prefer a monitored point after dark",
                        color = if (point.isMonitored) c.textMuted else c.error,
                    )
                }
            }
            BodyText("When", color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                state.days.forEachIndexed { index, day ->
                    Pill(day.label, selected = choice.dayIndex == index, onClick = { onDay(index) }, small = true)
                }
            }
            val free = state.days.getOrNull(choice.dayIndex)?.slots.orEmpty().filter { it.id !in taken }
            if (free.isEmpty()) {
                BodyText("No free hours left this day", color = c.textMuted)
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    free.forEach { slot ->
                        Pill(slot.label, selected = choice.slot?.id == slot.id, onClick = { onSlot(slot) }, small = true)
                    }
                }
            }
            BodyText("Payment", color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PaymentMethod.entries.forEach { method ->
                    Pill(method.label, selected = choice.payment == method, onClick = { onPayment(method) }, small = true)
                }
            }
            if (choice.missing.isNotEmpty()) {
                BodyText("Missing: ${choice.missing.joinToString(", ")}", color = c.error, modifier = Modifier.padding(top = 4.dp))
            }
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
    /** Part of the cart that could not be ordered, e.g. a listing someone else reserved first. */
    note: String? = null,
    onBackToHome: () -> Unit,
    onKeepShopping: () -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteExchange: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val products = order?.products.orEmpty()
    val exchangeProduct = order?.products?.singleOrNull()
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
        BodyText("Your campus swap is confirmed.", color = c.textMuted, modifier = Modifier.padding(bottom = if (note != null) 12.dp else 28.dp))
        if (note != null) {
            BodyText(note, color = c.text, modifier = Modifier.widthIn(max = 340.dp).padding(bottom = 24.dp))
        }

        StickyNote(modifier = Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(CampusIcons.Inbox, contentDescription = null, tint = c.textMuted, modifier = Modifier.padding(top = 2.dp).size(16.dp))
                Column {
                    BodyText("What happens next", color = c.text, weight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NextStep(CampusIcons.CheckCircle, "Seller has been notified")
                        NextStep(CampusIcons.Message, "1 · The seller confirms the meetup in chat")
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
            if (products.isNotEmpty()) {
                products.forEach { product ->
                    PrimaryButton(
                        text = if (products.size == 1) "Open the chat" else "Chat about ${product.title} · ${product.seller.name.substringBefore(' ')}",
                        onClick = { onOpenChat(product.id) },
                        leadingIcon = CampusIcons.Message,
                        contentPadding = PaddingValues(13.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (exchangeProduct != null) {
                    SecondaryButton(
                        text = "Already met? Complete exchange",
                        onClick = { onCompleteExchange(exchangeProduct.id) },
                        leadingIcon = CampusIcons.CheckCircle,
                        contentPadding = PaddingValues(13.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
