package com.campusswap.app.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusswap.app.components.ConditionBadge
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.VerifiedBadge
import com.campusswap.app.components.formatPrice
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.data.ChatMessage
import com.campusswap.app.data.MeetingProposal
import com.campusswap.app.data.MessageAuthor
import com.campusswap.app.data.MessageStatus
import com.campusswap.app.data.Product
import com.campusswap.app.data.ProposalStatus
import com.campusswap.app.ui.theme.AccentBlue
import com.campusswap.app.ui.theme.JetBrainsMonoFamily
import com.campusswap.app.ui.theme.SecondaryBlue
import com.campusswap.app.ui.theme.SuccessGreen

/**
 * View 09 — Buyer-Seller In-App Chat.
 * Pinned transaction header, real-time bubbles with delivery status, secure media button
 * and the "Propose Meeting Point" trigger that hands off to View 10.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    vm: AppViewModel,
    productId: String,
    onBack: () -> Unit,
    onViewListing: (String) -> Unit,
    onProposeMeeting: () -> Unit,
) {
    val product = remember(productId, vm.allProducts.size) { vm.allProducts.find { it.id == productId } }
    if (product == null) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            title = "Conversation not found",
            message = "This listing may have been removed.",
            actionLabel = "Go back",
            onAction = onBack,
        )
        return
    }

    val messages = vm.threadFor(product)
    val proposal = vm.meetingProposals[product.id]
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(38.dp).background(AccentBlue.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(product.seller.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (product.seller.isVerified) {
                                    Box(modifier = Modifier.size(16.dp)) { VerifiedBadge(compact = true) }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(7.dp).background(SuccessGreen, CircleShape))
                                Text(
                                    "Online · usually replies in 10 min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Shield, contentDescription = "Safety options", tint = AccentBlue)
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.imePadding()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProposeMeetingChip(
                            proposal = proposal,
                            onClick = onProposeMeeting,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(14.dp))
                        Text("No phone numbers shared", style = MaterialTheme.typography.labelMedium, color = SecondaryBlue)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Share a photo securely", tint = AccentBlue)
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message ${product.seller.name.substringBefore(' ')}…") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = SecondaryBlue.copy(alpha = 0.5f),
                            ),
                        )
                        val canSend = draft.isNotBlank()
                        IconButton(
                            onClick = {
                                vm.sendMessage(product, draft)
                                draft = ""
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(46.dp)
                                .background(if (canSend) AccentBlue else SecondaryBlue.copy(alpha = 0.35f), CircleShape),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TransactionHeader(product = product, onViewListing = { onViewListing(product.id) })
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { DayDivider("Today") }
                items(messages, key = { it.id }) { message ->
                    when {
                        message.proposal != null -> ProposalCard(message.proposal, onChange = onProposeMeeting)
                        message.author == MessageAuthor.SYSTEM -> SystemNotice(message.text)
                        else -> MessageBubble(message)
                    }
                }
            }
        }
    }
}

/** Pinned transaction overview: item photo, title, price and condition stay visible while chatting. */
@Composable
private fun TransactionHeader(product: Product, onViewListing: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onViewListing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductPlaceholderImage(
                category = product.category,
                seed = product.imageSeed,
                modifier = Modifier.size(52.dp),
                cornerRadius = 10,
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 3.dp)) {
                    Text(
                        formatPrice(product.price),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ConditionBadge(product.condition)
                }
                if (product.course != null) {
                    Text(
                        product.course.code,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Text("View", style = MaterialTheme.typography.labelLarge, color = AccentBlue)
        }
    }
}

@Composable
private fun DayDivider(label: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(color = SecondaryBlue.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun SystemNotice(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = SecondaryBlue, modifier = Modifier.size(13.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isMine = message.author == MessageAuthor.ME
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isMine) AccentBlue else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier.widthIn(max = 290.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        message.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMine) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isMine) StatusTicks(message.status)
                }
            }
        }
    }
}

/** Delivery status: clock (sending) → single tick (sent) → double tick (delivered) → light double tick (read). */
@Composable
private fun StatusTicks(status: MessageStatus) {
    val (icon, tint, label) = when (status) {
        MessageStatus.SENDING -> Triple(Icons.Outlined.Schedule, Color.White.copy(alpha = 0.6f), "Sending")
        MessageStatus.SENT -> Triple(Icons.Filled.Check, Color.White.copy(alpha = 0.75f), "Sent")
        MessageStatus.DELIVERED -> Triple(Icons.Filled.DoneAll, Color.White.copy(alpha = 0.75f), "Delivered")
        MessageStatus.READ -> Triple(Icons.Filled.DoneAll, Color(0xFFB9F6CA), "Read")
    }
    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
}

/** Meeting proposal rendered inline as a system card (View 10 hand-off result). */
@Composable
private fun ProposalCard(proposal: MeetingProposal, onChange: () -> Unit) {
    val accepted = proposal.status == ProposalStatus.ACCEPTED
    val accent = if (accepted) SuccessGreen else AccentBlue
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).background(accent.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        if (accepted) "Meeting confirmed" else "Meeting point proposed",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                    )
                    Text(
                        proposal.status.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(proposal.point.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${proposal.slot.day} · ${proposal.slot.label}",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Text("Verified safe zone · monitored", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
            }
            if (!accepted) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onChange,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = AccentBlue),
                    ) { Text("Change") }
                }
            }
        }
    }
}

@Composable
private fun ProposeMeetingChip(proposal: MeetingProposal?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val confirmed = proposal?.status == ProposalStatus.ACCEPTED
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (confirmed) SuccessGreen.copy(alpha = 0.14f) else AccentBlue.copy(alpha = 0.12f),
        contentColor = if (confirmed) SuccessGreen else AccentBlue,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                when {
                    confirmed -> "Meeting: ${proposal!!.point.name}"
                    proposal != null -> "Proposal pending"
                    else -> "Propose Meeting Point"
                },
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
