package com.campusswap.app.model.remote

import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    /** Always 204, whether or not the email has an account. */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<Unit>

    /** Signs the user in, like [login]. */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): LoginResponse

    @POST("auth/verify-email/send")
    suspend fun sendVerificationCode(): Response<Unit>

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): Response<Unit>
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val user: AuthUserDto, val accessToken: String)

data class RegisterRequest(val email: String, val password: String, val fullName: String, val major: String)

/** The backend may or may not sign the new user in; without a token the client logs in afterwards. */
data class RegisterResponse(val user: AuthUserDto?, val accessToken: String?)

data class AuthUserDto(val id: String, val email: String, val fullName: String)

data class ForgotPasswordRequest(val email: String)

data class ResetPasswordRequest(val email: String, val code: String, val newPassword: String)

data class VerifyEmailRequest(val code: String)

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): LoginResponse
    suspend fun register(request: RegisterRequest): RegisterResponse
    suspend fun forgotPassword(email: String)
    suspend fun resetPassword(request: ResetPasswordRequest): LoginResponse
    suspend fun sendVerificationCode()
    suspend fun verifyEmail(code: String)
}

class RetrofitAuthRemoteDataSource(private val api: AuthApi) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String): LoginResponse = api.login(LoginRequest(email, password))
    override suspend fun register(request: RegisterRequest): RegisterResponse = api.register(request)
    override suspend fun forgotPassword(email: String) = api.forgotPassword(ForgotPasswordRequest(email)).orThrow()
    override suspend fun resetPassword(request: ResetPasswordRequest): LoginResponse = api.resetPassword(request)
    override suspend fun sendVerificationCode() = api.sendVerificationCode().orThrow()
    override suspend fun verifyEmail(code: String) = api.verifyEmail(VerifyEmailRequest(code)).orThrow()

    private fun Response<Unit>.orThrow() {
        if (!isSuccessful) throw HttpException(this)
    }
}
