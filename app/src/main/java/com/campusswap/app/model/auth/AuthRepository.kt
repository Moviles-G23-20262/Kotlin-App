package com.campusswap.app.model.auth

import com.campusswap.app.model.remote.AuthRemoteDataSource
import com.campusswap.app.model.remote.LoginResponse
import com.campusswap.app.model.remote.RegisterRequest
import com.campusswap.app.model.remote.ResetPasswordRequest
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

/** Asking for, or confirming, a code sent by email. */
sealed interface EmailCodeResult {
    data object Done : EmailCodeResult
    /** A code was sent less than a minute ago. */
    data object TooSoon : EmailCodeResult
    /** Wrong or expired code, or the server couldn't send the email; [message] is ready to show. */
    data class Rejected(val message: String) : EmailCodeResult
    data object Offline : EmailCodeResult
}

sealed interface ResetPasswordResult {
    data class Success(val session: Session) : ResetPasswordResult
    data class Rejected(val message: String) : ResetPasswordResult
    data object Offline : ResetPasswordResult
}

class AuthRepository(
    private val remote: AuthRemoteDataSource,
    private val sessions: SessionManager,
) {

    suspend fun login(email: String, password: String): LoginResult = try {
        LoginResult.Success(startSession(remote.login(email.trim(), password)))
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

    /** The server answers the same for unknown emails, so [EmailCodeResult.Done] doesn't mean the account exists. */
    suspend fun forgotPassword(email: String): EmailCodeResult = emailCodeCall {
        remote.forgotPassword(email.trim().lowercase())
    }

    /** On success the user is signed in with the new password. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): ResetPasswordResult = try {
        val request = ResetPasswordRequest(email.trim().lowercase(), code.trim(), newPassword)
        ResetPasswordResult.Success(startSession(remote.resetPassword(request)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() in 400..499) {
            ResetPasswordResult.Rejected(serverMessage(e) ?: "Invalid or expired code")
        } else {
            ResetPasswordResult.Offline
        }
    } catch (e: IOException) {
        ResetPasswordResult.Offline
    }

    /** Emails the signed-in user a new verification code. */
    suspend fun sendVerificationCode(): EmailCodeResult = emailCodeCall { remote.sendVerificationCode() }

    suspend fun verifyEmail(code: String): EmailCodeResult = emailCodeCall { remote.verifyEmail(code.trim()) }

    fun logout() = sessions.signOut()

    private suspend fun startSession(response: LoginResponse): Session {
        val session = Session(response.accessToken, response.user.id, response.user.email, response.user.fullName)
        sessions.start(session)
        return session
    }

    private suspend fun emailCodeCall(call: suspend () -> Unit): EmailCodeResult = try {
        call()
        EmailCodeResult.Done
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            429 -> EmailCodeResult.TooSoon
            in 400..499 -> EmailCodeResult.Rejected(serverMessage(e) ?: "Invalid or expired code")
            // 503 with a message is the server saying the email couldn't go out, not that it's unreachable.
            503 -> serverMessage(e)?.let(EmailCodeResult::Rejected) ?: EmailCodeResult.Offline
            else -> EmailCodeResult.Offline
        }
    } catch (e: IOException) {
        EmailCodeResult.Offline
    }

    /** NestJS sends `message` as a string or as a list of validation messages. */
    private fun serverMessage(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val message = JsonParser.parseString(body).asJsonObject.get("message")
        val text = if (message.isJsonArray) message.asJsonArray.first().asString else message.asString
        text.replaceFirstChar { it.uppercase() }
    }.getOrNull()
}
