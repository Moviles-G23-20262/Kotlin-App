package com.campusswap.app.screens.home

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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.BackHeader
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.SecondaryButton
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.AppNotification
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.NotificationKind
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType


@Composable
fun NotificationsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenAlerts: () -> Unit,
    onProductClick: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onCompleteExchange: (String) -> Unit,
) {
    val c = CampusSwapTheme.colors
    val matchCount = vm.alertMatches.size
    val activeAlerts = vm.alerts.count { it.enabled }
    val pending = vm.pendingExchanges.filter { !it.isRated }

    LaunchedEffect(Unit) { vm.markNotificationsRead() }

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "Notifications", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // View 11 — the alert engine gets a permanent slot at the top of the inbox.
            item {
                SummaryCard(
                    icon = CampusIcons.Sparkle,
                    title = if (matchCount > 0) "$matchCount new ${if (matchCount == 1) "match" else "matches"}" else "No new matches",
                    message = if (activeAlerts > 0) {
                        "$activeAlerts active ${if (activeAlerts == 1) "alert" else "alerts"} watching the marketplace for you."
                    } else {
                        "Create an alert and we'll watch new listings for you."
                    },
                    actionLabel = if (activeAlerts > 0) "View alerts" else "Create an alert",
                    onAction = onOpenAlerts,
                )
            }

            // View 12 — anything bought but not yet rated is one tap from closing.
            if (pending.isNotEmpty()) {
                item {
                    val first = pending.first()
                    SummaryCard(
                        icon = CampusIcons.Package,
                        title = "${pending.size} exchange${if (pending.size == 1) "" else "s"} to close",
                        message = if (first.isScheduled) {
                            "Meeting confirmed for \"${first.product.title}\". Check the item, then rate ${first.product.seller.name.substringBefore(' ')}."
                        } else {
                            "Agree on a meeting point for \"${first.product.title}\", then close the exchange."
                        },
                        actionLabel = "Complete exchange",
                        onAction = { onCompleteExchange(first.product.id) },
                    )
                }
            }

            if (vm.notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        EmptyState(
                            icon = CampusIcons.Bell,
                            title = "No notifications",
                            message = "You're all caught up.",
                        )
                    }
                }
            } else {
                items(vm.notifications, key = { it.id }) { notification ->
                    NotificationRow(notification) {
                        val id = notification.productId
                        when (notification.kind) {
                            NotificationKind.ALERT_MATCH -> onOpenAlerts()
                            NotificationKind.CHAT -> id?.let(onOpenChat)
                            NotificationKind.EXCHANGE -> id?.let(onCompleteExchange)
                            NotificationKind.PRODUCT -> id?.let(onProductClick)
                            NotificationKind.GENERIC -> id?.let(onProductClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val c = CampusSwapTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard(borderColor = c.accentLo)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = c.accentHi, modifier = Modifier.size(16.dp))
            HeadingText(title, size = CampusType.sizeSm, maxLines = 1)
        }
        BodyText(message, modifier = Modifier.padding(top = 6.dp))
        SecondaryButton(
            text = actionLabel,
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            fontSize = CampusType.sizeXs,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    val c = CampusSwapTheme.colors
    val icon = when (notification.kind) {
        NotificationKind.ALERT_MATCH -> CampusIcons.Sparkle
        NotificationKind.CHAT -> CampusIcons.Message
        NotificationKind.EXCHANGE -> CampusIcons.Package
        NotificationKind.PRODUCT -> CampusIcons.Tag
        NotificationKind.GENERIC -> CampusIcons.Bell
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard()
            .plainClickable(onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = c.accentHi, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            BodyText(notification.title, color = c.text, weight = FontWeight.SemiBold)
            BodyText(notification.message, modifier = Modifier.padding(top = 2.dp))
        }
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(c.accent, CircleShape),
            )
        }
    }
}
