package com.campusswap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.Conversation
import com.campusswap.app.model.ConversationsLoad
import com.campusswap.app.model.ConversationsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val isOffline: Boolean = false,
    /** Set while the list is the copy saved on the phone because the server can't be reached. */
    val savedAtMillis: Long? = null,
)

class ConversationsViewModel(
    private val repository: ConversationsRepository,
    /** Emits true while the phone has a network; polling waits on it instead of failing every 10 s. */
    private val connection: Flow<Boolean>,
) : ViewModel() {
    private val _state = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    /**
     * Keeps the inbox fresh. The screen runs it only while it is visible (see `WhileVisible`): the
     * Seller Hub keeps one of these in the Profile tab, and it used to poll every 10 s even while the
     * user was on another tab or the app was in the background. Offline it loads the saved copy once
     * and then waits for the network.
     */
    suspend fun pollWhileVisible() {
        while (true) {
            refresh()
            delay(POLL_INTERVAL_MS)
            connection.first { it }
        }
    }

    suspend fun refresh() {
        when (val result = repository.conversations()) {
            is ConversationsLoad.Success -> _state.value = ConversationsUiState(
                isLoading = false,
                conversations = result.conversations,
                savedAtMillis = result.savedAtMillis,
            )
            ConversationsLoad.Offline ->
                _state.value = _state.value.copy(isLoading = false, isOffline = _state.value.conversations.isEmpty())
        }
    }

    companion object {
        const val POLL_INTERVAL_MS = 10_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ConversationsViewModel(container.conversationsRepository, container.connectivity.isOnline) }
        }
    }
}
