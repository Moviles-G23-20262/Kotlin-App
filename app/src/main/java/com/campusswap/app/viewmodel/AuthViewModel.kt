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
import com.campusswap.app.model.auth.EmailCodeResult
import com.campusswap.app.model.auth.LoginResult
import com.campusswap.app.model.auth.RegisterResult
import com.campusswap.app.model.auth.ResetPasswordResult
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository,
    /** Whether the phone has a working internet connection right now. */
    private val isDeviceOnline: () -> Boolean = { true },
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    fun login(email: String, password: String, onResult: (LoginResult) -> Unit) =
        run(onResult) { repository.login(email, password) }

    fun register(email: String, password: String, fullName: String, major: String, onResult: (RegisterResult) -> Unit) =
        run(onResult) { repository.register(email, password, fullName, major) }

    fun forgotPassword(email: String, onResult: (EmailCodeResult) -> Unit) =
        run(onResult) { repository.forgotPassword(email) }

    fun resetPassword(email: String, code: String, newPassword: String, onResult: (ResetPasswordResult) -> Unit) =
        run(onResult) { repository.resetPassword(email, code, newPassword) }

    fun sendVerificationCode(onResult: (EmailCodeResult) -> Unit) =
        run(onResult) { repository.sendVerificationCode() }

    fun verifyEmail(code: String, onResult: (EmailCodeResult) -> Unit) =
        run(onResult) { repository.verifyEmail(code) }

    /**
     * What to tell the user when a request didn't go through: the phone has no internet, or it has
     * internet but CampusSwap didn't answer. Read it when the result arrives.
     */
    val connectionProblem: String
        get() = if (isDeviceOnline()) SERVER_UNREACHABLE else NO_INTERNET

    /** One request at a time: taps while one is in flight are ignored. */
    private fun <T> run(onResult: (T) -> Unit, call: suspend () -> T) {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            val result = call()
            isLoading = false
            onResult(result)
        }
    }

    companion object {
        const val NO_INTERNET = "You don't have an internet connection. Connect to Wi-Fi or mobile data and try again."
        const val SERVER_UNREACHABLE = "CampusSwap isn't responding right now. Try again in a few minutes."

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AuthViewModel(container.authRepository) { container.connectivity.isOnline.value } }
        }
    }
}
