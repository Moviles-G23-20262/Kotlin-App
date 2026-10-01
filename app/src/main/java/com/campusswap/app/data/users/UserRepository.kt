package com.campusswap.app.data.users

import kotlinx.coroutines.CancellationException

// busca en los usuarios tengan correo

sealed interface UserLookup {
    data class Found(val userId: String, val fullName: String) : UserLookup
    data object NotFound : UserLookup
    data object Offline : UserLookup
}

class UserRepository(private val remote: UserRemoteDataSource) {

    suspend fun findByEmail(email: String): UserLookup = try {
        val user = remote.getUsers().firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        if (user == null) UserLookup.NotFound else UserLookup.Found(user.id, user.fullName)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        UserLookup.Offline
    }
}