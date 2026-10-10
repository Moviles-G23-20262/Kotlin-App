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
) {
    val canSend: Boolean get() = availability == ChatAvailability.READY && !isSending
}

class ChatViewModel(
    private val productId: String,
    private val chat: ChatRepository,
    private val proposals: MeetingProposalRepository,
    private val onProposalChanged: (String, MeetingProposal) -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var pending: List<ChatMessage> = emptyList()
    private var lastProposalStatus: ProposalStatus? = null
    private var firstContact = true

    init {
        startPolling()
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
            is ChatLoad.Success -> publish(result.messages)
            ChatLoad.NotSynced -> _state.update { it.copy(availability = ChatAvailability.NOT_SYNCED, isSending = false) }
            ChatLoad.Offline -> _state.update {
                it.copy(availability = if (it.messages.isEmpty()) ChatAvailability.OFFLINE else ChatAvailability.READY)
            }
        }
    }

    private fun publish(messages: List<ChatMessage>) {
        val confirmed = messages.mapTo(mutableSetOf()) { it.text }
        pending = pending.filterNot { it.text in confirmed }
        val proposal = chat.latestProposal(messages)
        if (messages.any { it.author == MessageAuthor.ME }) firstContact = false
        reportConfirmation(messages, proposal)
        _state.value = ChatUiState(
            availability = ChatAvailability.READY,
            messages = messages + pending,
            proposal = proposal,
            isSending = pending.isNotEmpty(),
            error = _state.value.error,
        )
        proposal?.let { onProposalChanged(productId, it) }
        viewModelScope.launch { chat.markRead(productId) }
    }

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

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || !_state.value.canSend) return
        val optimistic = ChatMessage(
            id = "pending-${System.currentTimeMillis()}",
            author = MessageAuthor.ME,
            text = trimmed,
            time = "",
            status = MessageStatus.SENDING,
            sentAt = Instant.now(),
        )
        pending = pending + optimistic
        _state.update { it.copy(messages = it.messages + optimistic, isSending = true, error = null) }

        viewModelScope.launch {
            when (val result = chat.send(productId, trimmed)) {
                is SendResult.Success -> {
                    val mine = _state.value.messages.count { it.author == MessageAuthor.ME }
                    if (firstContact) {
                        firstContact = false
                        Analytics.log(Events.CONTACT_SELLER, "product_id" to productId)
                    }
                    Analytics.log(Events.CHAT_MESSAGE_SENT, "product_id" to productId, "my_messages" to mine)
                    refresh()
                }
                SendResult.Offline -> failSend("No connection. Your message wasn't sent.")
                SendResult.NotSynced -> failSend("This listing isn't on the server, so the chat can't be opened.")
                is SendResult.Rejected -> failSend(result.message ?: "The server couldn't save your message.")
            }
        }
    }

    private fun failSend(reason: String) {
        pending = emptyList()
        _state.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.status == MessageStatus.SENDING },
                isSending = false,
                error = reason,
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

        fun factory(container: AppContainer, productId: String, onProposalChanged: (String, MeetingProposal) -> Unit) =
            viewModelFactory {
                initializer {
                    ChatViewModel(
                        productId = productId,
                        chat = container.chatRepository,
                        proposals = container.meetingProposalRepository,
                        onProposalChanged = onProposalChanged,
                    )
                }
            }
    }
}
