package com.campusswap.app.viewmodel

import com.campusswap.app.model.auth.AuthRepository
import com.campusswap.app.model.auth.FakeSessionStore
import com.campusswap.app.model.auth.LoginResult
import com.campusswap.app.model.auth.SessionManager
import com.campusswap.app.model.remote.AuthRemoteDataSource
import com.campusswap.app.model.remote.LoginResponse
import com.campusswap.app.model.remote.RegisterRequest
import com.campusswap.app.model.remote.RegisterResponse
import com.campusswap.app.model.remote.ResetPasswordRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Logging in without internet must say so, not blame the server. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private class FailingRemote(private val failure: () -> Throwable) : AuthRemoteDataSource {
        override suspend fun login(email: String, password: String): LoginResponse = throw failure()
        override suspend fun register(request: RegisterRequest): RegisterResponse = throw failure()
        override suspend fun forgotPassword(email: String) = throw failure()
        override suspend fun resetPassword(request: ResetPasswordRequest): LoginResponse = throw failure()
        override suspend fun sendVerificationCode() = throw failure()
        override suspend fun verifyEmail(code: String) = throw failure()
    }

    private fun kotlinx.coroutines.test.TestScope.viewModel(online: Boolean, failure: () -> Throwable) = AuthViewModel(
        AuthRepository(FailingRemote(failure), SessionManager(FakeSessionStore(), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), this)),
    ) { online }

    private fun AuthViewModel.loginMessage(): String? {
        var message: String? = null
        login("ana@uniandes.edu.co", "secret123") { result ->
            if (result == LoginResult.Offline) message = connectionProblem
        }
        return message
    }

    @Test fun withoutInternetTheLoginSaysThePhoneIsOffline() = runTest {
        val vm = viewModel(online = false) { UnknownHostException("no network") }

        assertEquals(AuthViewModel.NO_INTERNET, vm.loginMessage())
    }

    @Test fun withInternetButTheServerDownTheLoginSaysCampusSwapIsNotResponding() = runTest {
        val vm = viewModel(online = true) { HttpException(Response.error<Any>(503, ResponseBody.create(null, ""))) }

        assertEquals(AuthViewModel.SERVER_UNREACHABLE, vm.loginMessage())
    }
}
