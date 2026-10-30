package com.campusswap.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.campusswap.app.AppContainer
import com.campusswap.app.model.auth.AuthRepository
import com.campusswap.app.model.auth.LoginResult
import com.campusswap.app.model.auth.RegisterResult
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    fun login(email: String, password: String, onResult: (LoginResult) -> Unit) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = repository.login(email, password)
            isLoading = false
            onResult(result)
        }
    }

    fun register(email: String, password: String, fullName: String, major: String, onResult: (RegisterResult) -> Unit) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = repository.register(email, password, fullName, major)
            isLoading = false
            onResult(result)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AuthViewModel(container.authRepository) }
        }
    }
}
