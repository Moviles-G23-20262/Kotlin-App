package com.campusswap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.Meetup
import com.campusswap.app.model.MeetupsLoad
import com.campusswap.app.model.MeetupsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class MeetupsUiState(
    val isLoading: Boolean = true,
    val meetups: List<Meetup> = emptyList(),
    val isOffline: Boolean = false,
    val savedAtMillis: Long? = null,
)

class MeetupsViewModel(
    private val repository: MeetupsRepository,
    private val connection: Flow<Boolean>,
) : ViewModel() {
    private val _state = MutableStateFlow(MeetupsUiState())
    val state: StateFlow<MeetupsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.saved() ?: return@launch
            if (_state.value.isLoading) _state.value = MeetupsUiState(isLoading = false, meetups = saved.meetups)
        }
    }

    suspend fun refreshWhileVisible() {
        connection.distinctUntilChanged().collect { refresh() }
    }

    suspend fun refresh() {
        _state.value = when (val result = repository.load()) {
            is MeetupsLoad.Success -> MeetupsUiState(
                isLoading = false,
                meetups = result.meetups,
                savedAtMillis = result.savedAtMillis,
            )
            MeetupsLoad.Offline -> _state.value.copy(isLoading = false, isOffline = _state.value.meetups.isEmpty())
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { MeetupsViewModel(container.meetupsRepository, container.connectivity.isOnline) }
        }
    }
}
