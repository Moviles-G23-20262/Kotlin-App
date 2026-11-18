package com.campusswap.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.Seller
import com.campusswap.app.model.sellers.SellerProfile
import com.campusswap.app.model.sellers.SellerProfileLoad
import com.campusswap.app.model.sellers.SellerProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SellerProfileUiState(
    val isLoading: Boolean = true,
    val profile: SellerProfile? = null,
    /** Set while the profile shown is the copy saved on the phone (no connection). */
    val savedAtMillis: Long? = null,
    /** No connection and nothing saved for this seller. */
    val unavailable: Boolean = false,
)

class SellerProfileViewModel(
    private val sellerId: String,
    private val fallback: Seller?,
    private val repository: SellerProfileRepository,
    private val connection: Flow<Boolean>,
) : ViewModel() {
    private val _state = MutableStateFlow(SellerProfileUiState())
    val state: StateFlow<SellerProfileUiState> = _state.asStateFlow()

    init {
        // The saved copy appears right away; the network answer replaces it when it arrives.
        viewModelScope.launch {
            val saved = repository.saved(sellerId, fallback) ?: return@launch
            if (_state.value.isLoading) {
                _state.value = SellerProfileUiState(isLoading = false, profile = saved.profile, savedAtMillis = saved.savedAtMillis)
            }
        }
    }

    /** Reloads every time the connection changes, while the screen is visible. */
    suspend fun refreshWhileVisible() {
        connection.distinctUntilChanged().collect { refresh() }
    }

    suspend fun refresh(force: Boolean = false) {
        _state.value = when (val result = repository.load(sellerId, fallback, force)) {
            is SellerProfileLoad.Live -> SellerProfileUiState(isLoading = false, profile = result.profile)
            is SellerProfileLoad.Saved -> SellerProfileUiState(
                isLoading = false,
                profile = result.profile,
                savedAtMillis = result.savedAtMillis,
            )
            SellerProfileLoad.Unavailable -> _state.value.copy(isLoading = false, unavailable = _state.value.profile == null)
        }
    }

    companion object {
        fun factory(container: AppContainer, sellerId: String, fallback: Seller?) = viewModelFactory {
            initializer {
                SellerProfileViewModel(sellerId, fallback, container.sellerProfileRepository, container.connectivity.isOnline)
            }
        }
    }
}