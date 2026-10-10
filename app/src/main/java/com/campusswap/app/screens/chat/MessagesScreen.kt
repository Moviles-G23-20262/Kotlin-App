package com.campusswap.app.screens.chat

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.components.BackHeader
import com.campusswap.app.components.BodyText
import com.campusswap.app.components.CampusIcons
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.HeadingText
import com.campusswap.app.components.campusCard
import com.campusswap.app.components.plainClickable
import com.campusswap.app.data.Conversation
import com.campusswap.app.ui.theme.CampusSwapTheme
import com.campusswap.app.ui.theme.CampusType

@Composable
fun MessagesScreen(onBack: () -> Unit, onOpenChat: (String) -> Unit) {
    val c = CampusSwapTheme.colors
    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val vm: ConversationsViewModel = viewModel(factory = ConversationsViewModel.factory(container))
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        BackHeader(title = "Messages", onBack = onBack)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
            state.isOffline -> EmptyState(
                icon = CampusIcons.Message,
                title = "No connection",
                message = "We couldn't load your conversations. Check your connection and try again.",
                actionLabel = "Go back",
                onAction = onBack,
            )
            state.conversations.isEmpty() -> EmptyState(
                icon = CampusIcons.Message,
                title = "No conversations yet",
                message = "When you message a seller, or someone asks about one of your listings, the conversation shows up here.",
                actionLabel = "Go back",
                onAction = onBack,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.conversations, key = { it.productId }) { conversation ->
                    ConversationRow(conversation) { onOpenChat(conversation.productId) }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
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
            Icon(CampusIcons.Message, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeadingText(conversation.counterpartName, size = CampusType.sizeSm, maxLines = 1)
                if (conversation.sellingThis) SellingTag()
            }
            BodyText(conversation.productTitle, color = c.text2, maxLines = 1)
            BodyText(
                conversation.lastMessage ?: "No messages yet",
                color = if (conversation.unread > 0) c.text else c.textMuted,
                maxLines = 1,
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BodyText(conversation.time, color = c.textMuted)
            if (conversation.unread > 0) UnreadBadge(conversation.unread)
        }
    }
}

@Composable
private fun SellingTag() {
    val c = CampusSwapTheme.colors
    Surface(shape = RoundedCornerShape(6.dp), color = c.accentLo, contentColor = c.accent) {
        BodyText(
            "Selling",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = c.accent,
            weight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val c = CampusSwapTheme.colors
    Box(
        modifier = Modifier.size(22.dp).background(c.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BodyText(
            if (count > 9) "9+" else "$count",
            color = c.accentText,
            weight = FontWeight.Bold,
        )
    }
}
