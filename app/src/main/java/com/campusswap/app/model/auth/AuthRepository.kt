package com.campusswap.app.data.auth

import com.campusswap.app.data.remote.AuthRemoteDataSource
import com.campusswap.app.data.remote.RegisterRequest
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginResult {
    data class Success(val session: Session) : LoginResult
    data object InvalidCredentials : LoginResult
    data object Offline : LoginResult
}

sealed interface RegisterResult {
    data class Success(val session: Session) : RegisterResult
    data object EmailTaken : RegisterResult
    /** The server refused the data; [message] is its explanation, ready to show. */
    data class Rejected(val message: String) : RegisterResult
    data object Offline : RegisterResult
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

    suspend fun register(email: String, password: String, fullName: String, major: String): RegisterResult {
        val request = RegisterRequest(email.trim().lowercase(), password, fullName.trim(), major.trim())
        return try {
            val response = remote.register(request)
            val user = response.user
            if (response.accessToken != null && user != null) {
                val session = Session(response.accessToken, user.id, user.email, user.fullName)
                sessions.start(session)
                RegisterResult.Success(session)
            } else {
                when (val login = login(request.email, password)) {
                    is LoginResult.Success -> RegisterResult.Success(login.session)
                    LoginResult.InvalidCredentials -> RegisterResult.Rejected("Account created. Log in to continue.")
                    LoginResult.Offline -> RegisterResult.Offline
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> RegisterResult.EmailTaken
                in 400..499 -> RegisterResult.Rejected(serverMessage(e) ?: "Check your details and try again.")
                else -> RegisterResult.Offline
            }
        } catch (e: IOException) {
            RegisterResult.Offline
        }
    }

    fun logout() = sessions.signOut()

    /** NestJS sends `message` as a string or as a list of validation messages. */
    private fun serverMessage(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = JsonParser.parseString(body).asJsonObject.get("message")
        val text = if (message.isJsonArray) message.asJsonArray.first().asString else message.asString
        text.replaceFirstChar { it.uppercase() }
    }.getOrNull()
}
