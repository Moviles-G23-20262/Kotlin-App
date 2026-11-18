package com.campusswap.app.views.screens.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.Seller
import com.campusswap.app.model.sellers.SellerProfile
import com.campusswap.app.model.sellers.SellerReview
import com.campusswap.app.viewmodel.SellerProfileViewModel
import com.campusswap.app.views.components.BackHeader
import com.campusswap.app.views.components.Badge
import com.campusswap.app.views.components.BodyText
import com.campusswap.app.views.components.CampusIcons
import com.campusswap.app.views.components.EmptyState
import com.campusswap.app.views.components.HeadingText
import com.campusswap.app.views.components.InitialsAvatar
import com.campusswap.app.views.components.OfflineBanner
import com.campusswap.app.views.components.PriceText
import com.campusswap.app.views.components.ProductRowCard
import com.campusswap.app.views.components.PullToRefresh
import com.campusswap.app.views.components.SectionTitle
import com.campusswap.app.views.components.WhileVisible
import com.campusswap.app.views.components.campusCard
import com.campusswap.app.views.components.formatRating
import com.campusswap.app.views.components.timeAgo
import com.campusswap.app.views.themes.CampusSwapTheme
import com.campusswap.app.views.themes.CampusType

/** Public profile of a seller: reputation, reviews and what they are selling now. */
@Composable
fun SellerProfileScreen(
    sellerId: String,
    fallback: Seller?,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val vm: SellerProfileViewModel = viewModel(factory = SellerProfileViewModel.factory(container, sellerId, fallback))
    WhileVisible(vm) { vm.refreshWhileVisible() }
    val state by vm.state.collectAsState()
    val profile = state.profile

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "Seller profile", onBack = onBack)
        OfflineBanner(
            visible = state.savedAtMillis != null,
            message = "You're offline. This is the profile saved on this phone ${state.savedAtMillis?.let { timeAgo(it) }.orEmpty()}.",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )

        when {
            profile == null && state.unavailable -> EmptyState(
                icon = CampusIcons.User,
                title = "No connection",
                message = "This seller's profile hasn't been opened on this phone before. Connect to see their reviews.",
                actionLabel = "Go back",
                onAction = onBack,
            )
            profile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
            else -> PullToRefresh(onRefresh = { vm.refresh(force = true) }, modifier = Modifier.fillMaxSize()) {
                SellerProfileContent(profile, onProductClick)
            }
        }
    }
}

@Composable
private fun SellerProfileContent(profile: SellerProfile, onProductClick: (String) -> Unit) {
    val c = CampusSwapTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ProfileHeader(profile) }
        if (profile.reviewCount > 0) item { StarBreakdown(profile) }

        item {
            SectionTitle("Available now", icon = CampusIcons.Store, modifier = Modifier.padding(top = 8.dp))
        }
        if (profile.availableListings.isEmpty()) {
            item { BodyText("${firstName(profile.name)} has nothing for sale right now.", color = c.textMuted) }
        } else {
            items(profile.availableListings, key = { "listing-${it.id}" }) { product ->
                ProductRowCard(product = product, onClick = { onProductClick(product.id) }, thumbSize = 58.dp) {
                    BodyText(product.title, color = c.text, weight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.padding(bottom = 3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriceText(product.price)
                        Badge(product.condition.label)
                    }
                }
            }
        }

        item {
            SectionTitle("Reviews (${profile.reviewCount})", icon = CampusIcons.Star, modifier = Modifier.padding(top = 8.dp))
        }
        if (profile.reviews.isEmpty()) {
            item { BodyText("No reviews yet. Reviews appear after a completed exchange.", color = c.textMuted) }
        } else {
            items(profile.reviews, key = { "review-${it.id}" }) { review -> ReviewCard(review) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileHeader(profile: SellerProfile) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().campusCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InitialsAvatar(profile.name, size = 56.dp, fontSize = CampusType.sizeMd)
            Column(modifier = Modifier.weight(1f)) {
                HeadingText(profile.name, size = CampusType.sizeMd, maxLines = 1)
                listOfNotNull(profile.major, profile.faculty).takeIf { it.isNotEmpty() }?.let {
                    BodyText(it.joinToString(" · "), color = c.textMuted, maxLines = 2)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(profile.averageStars?.let { formatRating(it) } ?: "New", "Rating", Modifier.weight(1f))
            StatBox(profile.reviewCount.toString(), "Reviews", Modifier.weight(1f))
            StatBox(profile.soldCount.toString(), "Sold", Modifier.weight(1f))
            StatBox(profile.availableListings.size.toString(), "For sale", Modifier.weight(1f))
        }
        if (profile.topTags.isNotEmpty()) {
            BodyText("Buyers say", color = c.textMuted, size = CampusType.size2xs)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                profile.topTags.forEach { Badge("${it.tag} · ${it.count}", highlighted = true) }
            }
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = modifier.background(c.elevated, RoundedCornerShape(12.dp)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HeadingText(value, size = CampusType.sizeMd, color = c.text)
        BodyText(label, size = CampusType.size2xs, color = c.textMuted)
    }
}

/** One bar per number of stars, 5 at the top, as long as its share of the reviews. */
@Composable
private fun StarBreakdown(profile: SellerProfile) {
    val c = CampusSwapTheme.colors
    val most = profile.starCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(
        modifier = Modifier.fillMaxWidth().campusCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (stars in 5 downTo 1) {
            val count = profile.starCounts[stars - 1]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BodyText("$stars", color = c.text2, modifier = Modifier.width(10.dp))
                Icon(CampusIcons.Star, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(12.dp))
                Box(Modifier.weight(1f).height(8.dp).background(c.elevated, RoundedCornerShape(4.dp))) {
                    if (count > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / most)
                                .fillMaxHeight()
                                .background(c.accent, RoundedCornerShape(4.dp)),
                        )
                    }
                }
                BodyText("$count", color = c.textMuted, modifier = Modifier.width(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReviewCard(review: SellerReview) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().campusCard().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(5) { i ->
                    Icon(
                        if (i < review.stars) CampusIcons.Star else CampusIcons.StarOutline,
                        contentDescription = null,
                        tint = c.accentHi,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            review.createdAtMillis?.let { BodyText(timeAgo(it), size = CampusType.size2xs, color = c.textMuted) }
        }
        review.text?.let { BodyText(it, color = c.text, lineHeight = CampusType.sizeXs * 1.5f) }
        if (review.tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                review.tags.forEach { Badge(it) }
            }
        }
        BodyText("— ${review.raterName?.let(::firstName) ?: "A buyer"}", size = CampusType.size2xs, color = c.textMuted)
    }
}

private fun firstName(name: String) = name.trim().substringBefore(' ')