package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}

data class LoginRequest(val email: String, val password: String)

data class LoginResponse(val user: AuthUserDto, val accessToken: String)

data class AuthUserDto(val id: String, val email: String, val fullName: String)

interface AuthRemoteDataSource {
    suspend fun login(email: String, password: String): LoginResponse
}

class RetrofitAuthRemoteDataSource(private val api: AuthApi) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String): LoginResponse = api.login(LoginRequest(email, password))
}
