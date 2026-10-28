package com.campusswap.app.data.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock

class SessionManager(
    private val store: SessionStore,
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    var token: String? = null
        private set

    fun launchRestore() {
        scope.launch { restore() }
    }

    suspend fun restore() {
        val saved = store.read()
        if (saved == null || JwtExpiry.isExpired(saved.token, clock.instant())) {
            if (saved != null) store.clear()
            signedOut(expired = false)
        } else {
            signedIn(saved)
        }
    }

    suspend fun start(session: Session) {
        store.save(session)
        signedIn(session)
    }

    fun validSession(): Session? {
        val session = (_state.value as? SessionState.SignedIn)?.session ?: return null
        if (!JwtExpiry.isExpired(session.token, clock.instant())) return session
        expire()
        return null
    }

    fun signOut() = end(expired = false)

    fun expire() = end(expired = true)

    private fun end(expired: Boolean) {
        if (_state.value is SessionState.SignedOut) return
        signedOut(expired)
        scope.launch { store.clear() }
    }

    private fun signedIn(session: Session) {
        token = session.token
        _state.value = SessionState.SignedIn(session)
    }

    private fun signedOut(expired: Boolean) {
        token = null
        _state.value = SessionState.SignedOut(expired)
    }
}
