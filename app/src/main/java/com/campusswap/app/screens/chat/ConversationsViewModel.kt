package com.campusswap.app.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.data.Conversation
import com.campusswap.app.data.ConversationsLoad
import com.campusswap.app.data.ConversationsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val isOffline: Boolean = false,
)

class ConversationsViewModel(private val repository: ConversationsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ConversationsUiState())
    val state: StateFlow<ConversationsUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        pollJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    suspend fun refresh() {
        when (val result = repository.conversations()) {
            is ConversationsLoad.Success ->
                _state.value = ConversationsUiState(isLoading = false, conversations = result.conversations)
            ConversationsLoad.Offline ->
                _state.value = _state.value.copy(isLoading = false, isOffline = _state.value.conversations.isEmpty())
        }
    }

    companion object {
        const val POLL_INTERVAL_MS = 10_000L

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ConversationsViewModel(container.conversationsRepository) }
        }
    }
}
