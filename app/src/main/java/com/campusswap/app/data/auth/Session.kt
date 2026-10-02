package com.campusswap.app.data.auth

data class Session(
    val token: String,
    val userId: String,
    val email: String,
    val fullName: String,
)

sealed interface SessionState {
    data object Restoring : SessionState
    data class SignedIn(val session: Session) : SessionState
    data class SignedOut(val expired: Boolean = false) : SessionState
}
