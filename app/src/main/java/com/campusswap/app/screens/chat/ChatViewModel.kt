package com.campusswap.app.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.analytics.Analytics
import com.campusswap.app.analytics.Events
import com.campusswap.app.data.ChatLoad
import com.campusswap.app.data.ChatMessage
import com.campusswap.app.data.ChatRepository
import com.campusswap.app.data.MeetingProposal
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.MessageAuthor
import com.campusswap.app.data.MessageStatus
import com.campusswap.app.data.ProposalResult
import com.campusswap.app.data.ProposalStatus
import com.campusswap.app.data.SendResult
import com.campusswap.app.data.local.PendingMessageEntity
import com.campusswap.app.data.sync.OutboxRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class ChatAvailability { LOADING, READY, NOT_SYNCED, OFFLINE }

data class ChatUiState(
    val availability: ChatAvailability = ChatAvailability.LOADING,
    val messages: List<ChatMessage> = emptyList(),
    val proposal: MeetingProposal? = null,
    val isSending: Boolean = false,
    val error: String? = null,
    val counterpartName: String? = null,
) {
    val canSend: Boolean get() = availability == ChatAvailability.READY && !isSending
}

class ChatViewModel(
    private val productId: String,
    private val chat: ChatRepository,
    private val proposals: MeetingProposalRepository,
    private val outbox: OutboxRepository,
    private val currentUserId: () -> String?,
    private val isOnline: () -> Boolean,
    private val onProposalChanged: (String, MeetingProposal) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var delivered: List<ChatMessage> = emptyList()
    private var queued: List<PendingMessageEntity> = emptyList()
    private var lastProposalStatus: ProposalStatus? = null
    private var firstContact = true
    private var counterpart: String? = null

    init {
        startPolling()
        viewModelScope.launch {
            outbox.pendingMessages.collect { rows ->
                queued = rows.filter { it.materialId == productId && it.ownerId == currentUserId() }
                republish()
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    suspend fun refresh() {
        when (val result = chat.messages(productId)) {
            is ChatLoad.Success -> {
                if (counterpart == null) counterpart = chat.counterpartName(productId)
                publish(result.messages)
            }
            ChatLoad.NotSynced -> _state.update { it.copy(availability = ChatAvailability.NOT_SYNCED, isSending = false) }
            ChatLoad.Offline -> _state.update {
                it.copy(availability = if (it.messages.isEmpty()) ChatAvailability.OFFLINE else ChatAvailability.READY)
            }
        }
    }

    private fun publish(messages: List<ChatMessage>) {
        delivered = messages
        if (messages.any { it.author == MessageAuthor.ME }) firstContact = false
        reportConfirmation(messages, chat.latestProposal(messages))
        republish()
        viewModelScope.launch { chat.markRead(productId) }
    }

    /** The thread is what the server has, followed by whatever is still waiting in the outbox. */
    private fun republish() {
        val proposal = chat.latestProposal(delivered)
        _state.value = ChatUiState(
            availability = ChatAvailability.READY,
            messages = delivered + queued.map(::toPendingMessage),
            proposal = proposal,
            isSending = queued.isNotEmpty(),
            error = _state.value.error,
            counterpartName = counterpart,
        )
        proposal?.let { onProposalChanged(productId, it) }
    }

    private fun toPendingMessage(row: PendingMessageEntity) = ChatMessage(
        id = row.localId,
        author = MessageAuthor.ME,
        text = row.content,
        time = "",
        status = when {
            row.failedReason != null -> MessageStatus.FAILED
            isOnline() -> MessageStatus.SENDING
            else -> MessageStatus.QUEUED
        },
        sentAt = Instant.ofEpochMilli(row.createdAtMillis),
    )

    private fun reportConfirmation(messages: List<ChatMessage>, proposal: MeetingProposal?) {
        val status = proposal?.status
        if (status == ProposalStatus.ACCEPTED && lastProposalStatus != ProposalStatus.ACCEPTED) {
            Analytics.log(
                Events.MEETING_CONFIRMED,
                "product_id" to productId,
                "messages_in_thread" to messages.size,
                "elapsed_ms" to elapsedMs(messages),
            )
        }
        lastProposalStatus = status
    }

    private fun elapsedMs(messages: List<ChatMessage>): Long? {
        val stamps = messages.mapNotNull { it.sentAt }
        if (stamps.isEmpty()) return null
        return stamps.last().toEpochMilli() - stamps.first().toEpochMilli()
    }

    /**
     * The message goes to the outbox, so it survives having no signal and is sent by the
     * background sync. It appears in the thread right away as queued or sending.
     */
    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !_state.value.canSend) return
        val userId = currentUserId() ?: return

        viewModelScope.launch {
            outbox.queueMessage("pending-${System.currentTimeMillis()}", userId, productId, trimmed)
            if (firstContact) {
                firstContact = false
                Analytics.log(Events.CONTACT_SELLER, "product_id" to productId)
            }
            Analytics.log(
                Events.CHAT_MESSAGE_SENT,
                "product_id" to productId,
                "my_messages" to _state.value.messages.count { it.author == MessageAuthor.ME },
            )
        }
    }

    fun respondToProposal(accept: Boolean) {
        val proposal = _state.value.proposal ?: return
        viewModelScope.launch {
            when (val result = proposals.respond(proposal, accept)) {
                is ProposalResult.Success -> refresh()
                ProposalResult.Offline -> showError("No connection. Try again.")
                ProposalResult.NotSynced -> showError("This proposal isn't on the server.")
                is ProposalResult.Rejected -> showError(result.message ?: "The server couldn't save your answer.")
            }
        }
    }

    private fun showError(message: String) = _state.update { it.copy(error = message) }

    fun dismissError() = _state.update { it.copy(error = null) }

    companion object {
        const val POLL_INTERVAL_MS = 5_000L

        fun factory(
            container: AppContainer,
            productId: String,
            isOnline: () -> Boolean,
            onProposalChanged: (String, MeetingProposal) -> Unit,
        ) = viewModelFactory {
            initializer {
                ChatViewModel(
                    productId = productId,
                    chat = container.chatRepository,
                    proposals = container.meetingProposalRepository,
                    outbox = container.outboxRepository,
                    currentUserId = { container.sessionManager.validSession()?.userId },
                    isOnline = isOnline,
                    onProposalChanged = onProposalChanged,
                )
            }
        }
    }
}
