package com.campusswap.app.data.auth

import com.campusswap.app.data.remote.AuthRemoteDataSource
import com.campusswap.app.data.remote.AuthUserDto
import com.campusswap.app.data.remote.LoginResponse
import com.campusswap.app.data.remote.RegisterRequest
import com.campusswap.app.data.remote.RegisterResponse
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

    private class FakeAuthRemote(
        var registerOutcome: () -> RegisterResponse = { error("register not expected") },
        var outcome: () -> LoginResponse,
    ) : AuthRemoteDataSource {
        var lastEmail: String? = null
        var lastRegister: RegisterRequest? = null
        override suspend fun login(email: String, password: String): LoginResponse {
            lastEmail = email
            return outcome()
        }
        override suspend fun register(request: RegisterRequest): RegisterResponse {
            lastRegister = request
            return registerOutcome()
        }
    }

    private fun http(code: Int, body: String = "") = HttpException(Response.error<Any>(code, ResponseBody.create(null, body)))

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

    @Test fun registerWithTokenStartsTheSessionAndCleansInput() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val remote = FakeAuthRemote(
            outcome = { error("login not expected") },
            registerOutcome = { RegisterResponse(AuthUserDto("user-2", "ana@uniandes.edu.co", "Ana Ruiz"), token) },
        )
        val repository = AuthRepository(remote, SessionManager(store, Clock.fixed(now, ZoneOffset.UTC), this))

        val result = repository.register(" Ana@Uniandes.edu.co ", "secret123", " Ana Ruiz ", " Systems ")

        assertEquals(RegisterResult.Success(Session(token, "user-2", "ana@uniandes.edu.co", "Ana Ruiz")), result)
        assertEquals(RegisterRequest("ana@uniandes.edu.co", "secret123", "Ana Ruiz", "Systems"), remote.lastRegister)
        assertEquals(token, store.saved?.token)
    }

    @Test fun registerWithoutTokenLogsInAfterwards() = runTest(UnconfinedTestDispatcher()) {
        val remote = FakeAuthRemote(
            outcome = { LoginResponse(AuthUserDto("user-2", "ana@uniandes.edu.co", "Ana Ruiz"), token) },
            registerOutcome = { RegisterResponse(AuthUserDto("user-2", "ana@uniandes.edu.co", "Ana Ruiz"), null) },
        )
        val repository = AuthRepository(remote, SessionManager(FakeSessionStore(), Clock.fixed(now, ZoneOffset.UTC), this))

        val result = repository.register("ana@uniandes.edu.co", "secret123", "Ana Ruiz", "Systems")

        assertEquals("user-2", (result as RegisterResult.Success).session.userId)
        assertEquals("ana@uniandes.edu.co", remote.lastEmail)
    }

    @Test fun registerMapsServerErrors() = runTest(UnconfinedTestDispatcher()) {
        val sessions = SessionManager(FakeSessionStore(), Clock.fixed(now, ZoneOffset.UTC), this)
        fun repo(error: Exception) = AuthRepository(FakeAuthRemote(outcome = { error("unused") }, registerOutcome = { throw error }), sessions)

        assertEquals(RegisterResult.EmailTaken, repo(http(409)).register("a@b.edu.co", "secret123", "A B", "Law"))
        assertEquals(
            RegisterResult.Rejected("Password must be longer than or equal to 8 characters"),
            repo(http(400, """{"message":["password must be longer than or equal to 8 characters"],"statusCode":400}"""))
                .register("a@b.edu.co", "x", "A B", "Law"),
        )
        assertEquals(RegisterResult.Offline, repo(http(500)).register("a@b.edu.co", "secret123", "A B", "Law"))
        assertEquals(RegisterResult.Offline, repo(IOException("down")).register("a@b.edu.co", "secret123", "A B", "Law"))
    }
}
