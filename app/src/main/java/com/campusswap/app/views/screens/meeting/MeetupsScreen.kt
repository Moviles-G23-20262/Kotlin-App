package com.campusswap.app.views.screens.meeting

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.model.Meetup
import com.campusswap.app.viewmodel.MeetupsViewModel
import com.campusswap.app.views.components.BackHeader
import com.campusswap.app.views.components.BodyText
import com.campusswap.app.views.components.CampusIcons
import com.campusswap.app.views.components.EmptyState
import com.campusswap.app.views.components.HeadingText
import com.campusswap.app.views.components.OfflineBanner
import com.campusswap.app.views.components.PullToRefresh
import com.campusswap.app.views.components.WhileVisible
import com.campusswap.app.views.components.campusCard
import com.campusswap.app.views.components.plainClickable
import com.campusswap.app.views.components.timeAgo
import com.campusswap.app.views.themes.CampusSwapTheme
import com.campusswap.app.views.themes.CampusType
import java.time.Duration

@Composable
fun MeetupsScreen(onBack: () -> Unit, onOpenChat: (String, String) -> Unit) {
    val c = CampusSwapTheme.colors
    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val vm: MeetupsViewModel = viewModel(factory = MeetupsViewModel.factory(container))
    WhileVisible(vm) { vm.refreshWhileVisible() }
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "My meetups", onBack = onBack)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
            state.isOffline -> EmptyState(
                icon = CampusIcons.Calendar,
                title = "No connection",
                message = "We couldn't load your meetups. Check your connection and try again.",
                actionLabel = "Go back",
                onAction = onBack,
            )
            state.meetups.isEmpty() -> EmptyState(
                icon = CampusIcons.Calendar,
                title = "No meetups yet",
                message = "When you propose or accept a meeting point in a chat, it shows up here.",
                actionLabel = "Go back",
                onAction = onBack,
            )
            else -> PullToRefresh(onRefresh = { vm.refresh() }, modifier = Modifier.fillMaxSize()) {
                val now = System.currentTimeMillis()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.savedAtMillis?.let { savedAt ->
                        item(key = "offline") {
                            OfflineBanner(
                                visible = true,
                                message = "You're offline. Showing your meetups as they were ${timeAgo(savedAt)}.",
                            )
                        }
                    }
                    items(state.meetups, key = { it.proposalId }) { meetup ->
                        MeetupRow(meetup, now) { onOpenChat(meetup.productId, meetup.roomId) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetupRow(meetup: Meetup, now: Long, onClick: () -> Unit) {
    val c = CampusSwapTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .campusCard()
            .plainClickable(onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(c.accentLo, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(CampusIcons.MapPin, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HeadingText(meetup.pointName, size = CampusType.sizeSm, maxLines = 1)
            BodyText("${meetup.slot.day} · ${meetup.slot.label}", color = c.text2, maxLines = 1)
            BodyText("With ${meetup.counterpartName} · ${meetup.productTitle}", color = c.textMuted, maxLines = 1)
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BodyText(startsIn(meetup, now), color = c.text, weight = FontWeight.SemiBold)
            when {
                meetup.confirmed -> StatusTag("Confirmed", c.success)
                meetup.proposedByMe -> StatusTag("Waiting for ${meetup.counterpartName.substringBefore(' ')}", c.warning)
                else -> StatusTag("Your answer", c.accent)
            }
        }
    }
}

@Composable
private fun StatusTag(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.14f), contentColor = color) {
        BodyText(
            text,
            color = color,
            weight = FontWeight.SemiBold,
            size = CampusType.size2xs,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun startsIn(meetup: Meetup, nowMillis: Long): String {
    val start = meetup.slot.startsAt?.toEpochMilli() ?: return ""
    val minutes = Duration.ofMillis(start - nowMillis).toMinutes()
    val days = minutes / (24 * 60)
    return when {
        minutes <= 0 -> "Now"
        minutes < 60 -> "In $minutes min"
        days < 1 -> "In ${minutes / 60} h"
        days == 1L -> "In 1 day"
        else -> "In $days days"
    }
}
