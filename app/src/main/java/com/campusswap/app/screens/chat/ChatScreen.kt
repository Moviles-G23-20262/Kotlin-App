package com.campusswap.app.screens.chat

import com.campusswap.app.domain.InputLimits
import com.campusswap.app.domain.InputValidation
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
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
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
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.campusswap.app.CampusSwapApplication
import com.campusswap.app.components.ConditionBadge
import com.campusswap.app.components.EmptyState
import com.campusswap.app.components.RemoteImage
import com.campusswap.app.components.plainClickable
import com.campusswap.app.components.ProductPlaceholderImage
import com.campusswap.app.components.VerifiedBadge
import com.campusswap.app.components.formatPrice
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.data.AppViewModel
import java.io.File
import com.campusswap.app.data.ChatMessage
import com.campusswap.app.data.MeetingProposal
import com.campusswap.app.data.MessageAuthor
import com.campusswap.app.data.MessageStatus
import com.campusswap.app.components.OfflineBanner
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
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
    onCompleteExchange: () -> Unit,
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

    val context = LocalContext.current
    val container = (context.applicationContext as CampusSwapApplication).container
    val chat: ChatViewModel = viewModel(
        factory = ChatViewModel.factory(container, product.id, isOnline = { vm.isOnline }) { id, proposal ->
            vm.cacheProposal(id, proposal)
        },
    )
    val state by chat.state.collectAsState()
    val messages = state.messages
    val counterpart = state.counterpartName ?: product.seller.name

    var showPhotoOptions by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<Uri?>(null) }

    fun sendFrom(uri: Uri) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        chat.sendPhoto(bytes, context.contentResolver.getType(uri) ?: "image/jpeg")
    }

    val pickFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::sendFrom)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) cameraTarget?.let(::sendFrom)
    }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                PhotoOption(Icons.Outlined.PhotoCamera, "Take a photo") {
                    showPhotoOptions = false
                    cameraTarget = newPhotoUri(context)
                    cameraTarget?.let(takePhoto::launch)
                }
                PhotoOption(Icons.Outlined.AddPhotoAlternate, "Choose from gallery") {
                    showPhotoOptions = false
                    pickFromGallery.launch("image/*")
                }
            }
        }
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
                                Text(counterpart, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    // The change can be closed once a meeting is agreed (Views 9/10) or once the item was paid for at checkout and is still waiting to be finish (View 12)
                    if (state.proposal?.status == ProposalStatus.ACCEPTED || vm.hasPendingExchange(product.id)) {
                        Surface(
                            onClick = onCompleteExchange,
                            color = AccentBlue.copy(alpha = 0.12f),
                            contentColor = AccentBlue,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(
                                    "Met already? Complete the exchange and rate each other",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("Open", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    OfflineBanner(
                        visible = !vm.isOnline,
                        message = "You're offline. Messages will be sent when you reconnect.",
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProposeMeetingChip(
                            proposal = state.proposal,
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
                        IconButton(
                            onClick = { showPhotoOptions = true },
                            enabled = state.availability == ChatAvailability.READY && !state.isUploadingPhoto,
                        ) {
                            if (state.isUploadingPhoto) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentBlue)
                            } else {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = "Share a photo securely", tint = AccentBlue)
                            }
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = InputValidation.sanitizeText(it, InputLimits.MESSAGE_MAX) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message ${counterpart.substringBefore(' ')}…") },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentBlue,
                                unfocusedBorderColor = SecondaryBlue.copy(alpha = 0.5f),
                            ),
                        )
                        val canSend = draft.isNotBlank() && state.canSend
                        IconButton(
                            onClick = {
                                chat.send(draft)
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
            when (state.availability) {
                ChatAvailability.LOADING -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
                ChatAvailability.NOT_SYNCED -> Box(Modifier.weight(1f)) {
                    EmptyState(
                        icon = Icons.Outlined.Person,
                        title = "Chat unavailable",
                        message = "This listing is sample data and is not on the server yet, so no conversation can be opened.",
                        actionLabel = "Go back",
                        onAction = onBack,
                    )
                }
                ChatAvailability.OFFLINE -> Box(Modifier.weight(1f)) {
                    EmptyState(
                        icon = Icons.Outlined.Person,
                        title = "No connection",
                        message = "We couldn't load this conversation. Check your connection and try again.",
                        actionLabel = "Go back",
                        onAction = onBack,
                    )
                }
                // Newest first and reversed, so the thread stays pinned to the latest message
                // and the keyboard pushes it up instead of covering it.
                ChatAvailability.READY -> LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages.asReversed(), key = { it.id }) { message ->
                    when {
                        message.proposal != null -> ProposalCard(
                            proposal = message.proposal,
                            canRespond = message.proposal.status == ProposalStatus.PENDING &&
                                message.proposal.proposerId != null &&
                                message.proposal.proposerId != vm.currentUserId,
                            error = state.error,
                            onChange = onProposeMeeting,
                            onRespond = { accept -> chat.respondToProposal(accept) },
                        )
                        message.author == MessageAuthor.SYSTEM -> SystemNotice(message.text)
                        else -> MessageBubble(message)
                    }
                }
                item { DayDivider("Today") }
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
                if (message.imageUrl != null) {
                    RemoteImage(
                        url = message.imageUrl,
                        contentDescription = "Photo shared in the chat",
                        modifier = Modifier
                            .padding(top = 2.dp, bottom = 4.dp)
                            .size(width = 220.dp, height = 165.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Text(message.text, style = MaterialTheme.typography.bodyMedium)
                }
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

/** Delivery status: cloud (queued offline) → clock (sending) → single tick (sent) → double tick (delivered) → light double tick (read). */
@Composable
private fun StatusTicks(status: MessageStatus) {
    val (icon, tint, label) = when (status) {
        MessageStatus.QUEUED -> Triple(Icons.Outlined.CloudOff, Color.White.copy(alpha = 0.6f), "Waiting for connection")
        MessageStatus.SENDING -> Triple(Icons.Outlined.Schedule, Color.White.copy(alpha = 0.6f), "Sending")
        MessageStatus.SENT -> Triple(Icons.Filled.Check, Color.White.copy(alpha = 0.75f), "Sent")
        MessageStatus.DELIVERED -> Triple(Icons.Filled.DoneAll, Color.White.copy(alpha = 0.75f), "Delivered")
        MessageStatus.READ -> Triple(Icons.Filled.DoneAll, Color(0xFFB9F6CA), "Read")
        MessageStatus.FAILED -> Triple(Icons.Outlined.ErrorOutline, Color(0xFFFFCDD2), "Not sent")
    }
    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(14.dp))
}

/** Meeting proposal rendered inline as a system card (View 10 hand-off result). */
@Composable
private fun ProposalCard(
    proposal: MeetingProposal,
    canRespond: Boolean,
    error: String?,
    onChange: () -> Unit,
    onRespond: (accept: Boolean) -> Unit,
) {
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
            if (canRespond) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onRespond(false) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = AccentBlue),
                    ) { Text("Decline") }
                    Button(
                        onClick = { onRespond(true) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    ) { Text("Accept") }
                }
            } else if (!accepted) {
                Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onChange,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = AccentBlue),
                    ) { Text(if (proposal.status == ProposalStatus.DECLINED) "Propose another" else "Change") }
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
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

@Composable
private fun PhotoOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().plainClickable(onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AccentBlue)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun newPhotoUri(context: Context): Uri {
    val folder = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(folder, "chat-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
}
