package com.campusswap.app.data.auth

import com.campusswap.app.data.remote.AuthRemoteDataSource
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data object InvalidCredentials : LoginResult
    data object Offline : LoginResult
}

class AuthRepository(
    private val remote: AuthRemoteDataSource,
    private val sessions: SessionManager,
) {

    suspend fun login(email: String, password: String): LoginResult = try {
        val response = remote.login(email.trim(), password)
        val session = Session(response.accessToken, response.user.id, response.user.email, response.user.fullName)
        sessions.start(session)
        LoginResult.Success(session)
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() in 400..499) LoginResult.InvalidCredentials else LoginResult.Offline
    } catch (e: IOException) {
        LoginResult.Offline
    }

    fun logout() = sessions.signOut()
}
