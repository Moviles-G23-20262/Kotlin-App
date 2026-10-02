package com.campusswap.app.data.auth

import com.campusswap.app.data.remote.AuthRemoteDataSource
import com.campusswap.app.data.remote.AuthUserDto
import com.campusswap.app.data.remote.LoginResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {
    private val now = Instant.parse("2026-10-02T15:00:00Z")
    private val token = jwtExpiringAt(now.plusSeconds(3600))

    private class FakeAuthRemote(var outcome: () -> LoginResponse) : AuthRemoteDataSource {
        var lastEmail: String? = null
        override suspend fun login(email: String, password: String): LoginResponse {
            lastEmail = email
            return outcome()
        }
    }

    private fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))

    @Test fun successfulLoginStoresTheSession() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val remote = FakeAuthRemote { LoginResponse(AuthUserDto("user-1", "ana@uniandes.edu.co", "Ana"), token) }
        val repository = AuthRepository(remote, SessionManager(store, Clock.fixed(now, ZoneOffset.UTC), this))

        val result = repository.login("  ana@uniandes.edu.co ", "secret123")

        assertEquals(LoginResult.Success(Session(token, "user-1", "ana@uniandes.edu.co", "Ana")), result)
        assertEquals("ana@uniandes.edu.co", remote.lastEmail)
        assertEquals(token, store.saved?.token)
    }

    @Test fun rejectedCredentialsDoNotStoreAnything() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val repository = AuthRepository(FakeAuthRemote { throw http(401) }, SessionManager(store, Clock.fixed(now, ZoneOffset.UTC), this))

        assertEquals(LoginResult.InvalidCredentials, repository.login("ana@uniandes.edu.co", "wrong"))
        assertNull(store.saved)
    }

    @Test fun networkOrServerFailuresAreReportedAsOffline() = runTest(UnconfinedTestDispatcher()) {
        val sessions = SessionManager(FakeSessionStore(), Clock.fixed(now, ZoneOffset.UTC), this)

        assertEquals(LoginResult.Offline, AuthRepository(FakeAuthRemote { throw IOException("airplane") }, sessions).login("a@b.co", "x"))
        assertEquals(LoginResult.Offline, AuthRepository(FakeAuthRemote { throw http(503) }, sessions).login("a@b.co", "x"))
    }

    @Test fun logoutSignsOut() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val sessions = SessionManager(store, Clock.fixed(now, ZoneOffset.UTC), this)
        val repository = AuthRepository(FakeAuthRemote { LoginResponse(AuthUserDto("user-1", "a@b.co", "A"), token) }, sessions)
        repository.login("a@b.co", "secret123")

        repository.logout()

        assertEquals(SessionState.SignedOut(expired = false), sessions.state.value)
        assertNull(store.saved)
    }
}
