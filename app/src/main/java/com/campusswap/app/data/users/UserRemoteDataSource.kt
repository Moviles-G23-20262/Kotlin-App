package com.campusswap.app.data.users

import retrofit2.http.GET

// busca en los usuarios tengan correo

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
)

interface UserRemoteDataSource {
    suspend fun getUsers(): List<UserDto>
}

interface UsersApi {
    @GET("users")
    suspend fun getUsers(): List<UserDto>
}

class RetrofitUserRemoteDataSource(private val api: UsersApi) : UserRemoteDataSource {
    override suspend fun getUsers() = api.getUsers()
}