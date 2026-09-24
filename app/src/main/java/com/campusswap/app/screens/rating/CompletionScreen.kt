package com.campusswap.app.screens.rating

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.components.Badge
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusHeader
import com.campusswap.app.components.CampusIconButton
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.CampusTextField
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.InitialsAvatar
import com.campusswap.app.components.MonoText
import com.campusswap.app.components.Pill
import com.campusswap.app.components.PriceText
import com.campusswap.app.components.PrimaryButton
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.SecondaryButton
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.CheckItem
import com.campusswap.app.data.Condition
import com.campusswap.app.data.FeedbackTag
import com.campusswap.app.data.Product
import com.campusswap.app.data.TransactionRating
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

/**
 * View 12 — Transaction Completion & Mutual Rating.
 * Condition checklist first, then stars, feedback tags and an optional written review.
 * Scores stay hidden until both sides rate, so nobody rates in retaliation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletionScreen(
    vm: AppViewModel,
    productId: String,
    onClose: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    val product = remember(productId, vm.allProducts.size) { vm.allProducts.find { it.id == productId } }

    if (product == null) {
        Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = CampusIcons.Package,
                title = "Exchange not found",
                message = "This listing may have been removed.",
                actionLabel = "Go back",
                onAction = onClose,
            )
        }
        return
    }

    val existing = vm.ratingFor(productId)
    val proposal = vm.meetingProposals[productId]
    // The exchange can be reached from the chat (Views 9/10) or straight from an
    // order placed at checkout, so the summary reflects whichever one applies.
    val order = vm.orderFor(productId)

    var checks by remember { mutableStateOf(setOf<CheckItem>()) }
    var confirmedCondition by remember { mutableStateOf(product.condition) }
    var stars by remember { mutableIntStateOf(0) }
    var tags by remember { mutableStateOf(setOf<FeedbackTag>()) }
    var review by remember { mutableStateOf("") }

    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val target = ExchangeTarget(
        productId = product.id,
        buyerId = vm.currentUser.id,
        sellerId = product.seller.id,
        price = product.price,
        meetingPointId = proposal?.point?.id,
        meetingPoint = proposal?.point?.location,
    )
    val checkIn: ExchangeCheckInViewModel = viewModel(factory = ExchangeCheckInViewModel.factory(container, target))
    val checkInState by checkIn.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        CampusHeader {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampusIconButton(CampusIcons.Close, contentDescription = "Close", onClick = onClose, iconSize = 18.dp)
                HeadingText(
                    text = if (existing == null) "Complete exchange" else "Exchange completed",
                    size = CampusType.sizeMd,
                )
            }
        }

        if (existing != null) {
            RatingSent(rating = existing, product = product, onClose = onClose)
        } else {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Transaction summary
            Column(modifier = Modifier.fillMaxWidth().campusCard().padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProductPlaceholderImage(
                        category = product.category,
                        seed = product.imageSeed,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 10,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        BodyText(product.title, color = c.text, weight = FontWeight.SemiBold, maxLines = 2)
                        PriceText(product.price, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(CampusIcons.MapPin, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(14.dp))
                    BodyText(
                        when {
                            proposal != null -> "${proposal.point.name} · ${proposal.slot.label}"
                            order != null -> "Meetup not agreed yet · paid at checkout"
                            else -> "Met on campus"
                        },
                        size = CampusType.size2xs,
                        color = c.textMuted,
                        modifier = Modifier.weight(1f),
                    )
                    MonoText(
                        order?.let { "CSW-${it.number}" }
                            ?: "TX-${product.id.uppercase()}-${product.imageSeed}42"
                    )
                }
            }

            StepCard(number = 1, title = "Confirm you're at the meeting point") {
                ExchangeCheckIn(viewModel = checkIn, pointName = proposal?.point?.name)
            }

            // Step 2 — condition checklist
            StepCard(number = 2, title = "Check the item before you pay") {
                CheckItem.entries.forEach { item ->
                    CheckRow(
                        label = if (item == CheckItem.CONDITION_OK) {
                            "Condition is ${product.condition.label} as listed"
                        } else {
                            item.label
                        },
                        checked = checks.contains(item),
                        onToggle = {
                            checks = if (checks.contains(item)) checks - item else checks + item
                        },
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BodyText("Something's off?", size = CampusType.size2xs, color = c.textMuted)
                    BodyText(
                        "Report a problem",
                        size = CampusType.size2xs,
                        color = c.error,
                        weight = FontWeight.SemiBold,
                        modifier = Modifier.plainClickable {},
                    )
                }
                BodyText(
                    "Confirm the condition you received",
                    color = c.text2,
                    weight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Condition.entries.forEach { condition ->
                        Pill(
                            condition.label,
                            selected = confirmedCondition == condition,
                            onClick = { confirmedCondition = condition },
                            small = true,
                        )
                    }
                }
                if (confirmedCondition != product.condition) {
                    BodyText(
                        "We'll flag the difference to the seller before the rating is published.",
                        size = CampusType.size2xs,
                        color = c.error,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // Step 3 — rating
            StepCard(number = 3, title = "Rate ${product.seller.name.substringBefore(' ')}") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { index ->
                        Icon(
                            imageVector = if (index <= stars) CampusIcons.Star else CampusIcons.StarOutline,
                            contentDescription = "$index stars",
                            tint = if (index <= stars) c.accentHi else c.borderSubtle,
                            modifier = Modifier
                                .size(36.dp)
                                .plainClickable { stars = index }
                                .padding(4.dp),
                        )
                    }
                    BodyText(
                        starLabel(stars),
                        color = c.text2,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                BodyText(
                    "What went well?",
                    color = c.text2,
                    weight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeedbackTag.entries.forEach { tag ->
                        Pill(
                            tag.label,
                            selected = tags.contains(tag),
                            onClick = { tags = if (tags.contains(tag)) tags - tag else tags + tag },
                            small = true,
                        )
                    }
                }
                CampusTextField(
                    value = review,
                    onValueChange = { if (it.length <= 200) review = it },
                    placeholder = "Add a short review (optional)",
                    singleLine = false,
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                MonoText("${review.length}/200", modifier = Modifier.padding(top = 4.dp))
            }

            BodyText(
                "Ratings stay hidden until you both rate, or 48 hours pass.",
                size = CampusType.size2xs,
                color = c.textMuted,
            )
        }

        val confirmed = checkInState is CheckInState.Confirmed
        val ready = confirmed && checks.size == CheckItem.entries.size && stars > 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(16.dp),
        ) {
            PrimaryButton(
                text = when {
                    ready -> "Send rating"
                    !confirmed -> "Confirm the exchange to continue"
                    else -> "Check the item to continue"
                },
                enabled = ready,
                onClick = {
                    vm.submitRating(
                        TransactionRating(
                            productId = productId,
                            stars = stars,
                            confirmedCondition = confirmedCondition,
                            tags = tags,
                            review = review.trim(),
                        )
                    )
                },
                contentPadding = PaddingValues(13.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        }
    }
}

@Composable
private fun RatingSent(rating: TransactionRating, product: Product, onClose: () -> Unit) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(c.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(CampusIcons.Check, contentDescription = null, tint = c.accentText, modifier = Modifier.size(34.dp))
        }
        HeadingText(
            "Exchange closed",
            size = CampusType.sizeLg,
            weight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 6.dp),
        )
        BodyText(
            "You rated ${product.seller.name} ${rating.stars}.0 — thanks for keeping the campus score honest.",
            color = c.textMuted,
            modifier = Modifier.padding(bottom = 20.dp),
        )

        Column(modifier = Modifier.fillMaxWidth().campusCard().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsAvatar(product.seller.name, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    BodyText(product.seller.name, color = c.text, weight = FontWeight.SemiBold)
                    BodyText(
                        if (rating.revealed) "Rated you 5.0 · Punctual" else "Hasn't rated you yet",
                        size = CampusType.size2xs,
                        color = c.textMuted,
                    )
                }
                if (rating.revealed) {
                    Icon(CampusIcons.Star, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(16.dp))
                }
            }
            if (rating.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rating.tags.take(3).forEach { Badge(it.label) }
                }
            }
            if (rating.review.isNotBlank()) {
                BodyText(rating.review, modifier = Modifier.padding(top = 12.dp))
            }
        }

        SecondaryButton(
            text = "Done",
            onClick = onClose,
            contentPadding = PaddingValues(13.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        )
    }
}

@Composable
private fun StepCard(number: Int, title: String, content: @Composable () -> Unit) {
    val c = CampusSwapTheme.colors
    Column(modifier = Modifier.fillMaxWidth().campusCard().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Box(
                modifier = Modifier.size(22.dp).background(c.accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                MonoText(number.toString(), color = c.accentText)
            }
            HeadingText(title, size = CampusType.sizeSm, maxLines = 1)
        }
        content()
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(if (checked) c.accent else c.elevated, RoundedCornerShape(6.dp))
                .border(1.dp, if (checked) c.accentLo else c.borderSubtle, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(CampusIcons.Check, contentDescription = null, tint = c.accentText, modifier = Modifier.size(13.dp))
            }
        }
        BodyText(label, color = if (checked) c.text else c.text2, size = CampusType.sizeXs)
    }
}

private fun starLabel(stars: Int): String = when (stars) {
    0 -> "Tap to rate"
    1 -> "Poor"
    2 -> "Could be better"
    3 -> "Fine"
    4 -> "Good"
    else -> "Excellent"
}
