package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val user: AuthUserDto, val accessToken: String)

data class RegisterRequest(val email: String, val password: String, val fullName: String, val major: String)

/** The backend may or may not sign the new user in; without a token the client logs in afterwards. */
data class RegisterResponse(val user: AuthUserDto?, val accessToken: String?)

data class AuthUserDto(val id: String, val email: String, val fullName: String)

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): LoginResponse
    suspend fun register(request: RegisterRequest): RegisterResponse
}

class RetrofitAuthRemoteDataSource(private val api: AuthApi) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String): LoginResponse = api.login(LoginRequest(email, password))
    override suspend fun register(request: RegisterRequest): RegisterResponse = api.register(request)
}
