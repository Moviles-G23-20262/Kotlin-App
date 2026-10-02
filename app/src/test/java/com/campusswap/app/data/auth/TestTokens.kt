package com.campusswap.app.data.auth

import java.time.Instant
import java.util.Base64

fun jwtExpiringAt(expiresAt: Instant): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val header = encoder.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
    val payload = encoder.encodeToString("""{"sub":"user-1","exp":${expiresAt.epochSecond}}""".toByteArray())
    return "$header.$payload.signature"
}

class FakeSessionStore(var saved: Session? = null) : SessionStore {
    override suspend fun read() = saved
    override suspend fun save(session: Session) {
        saved = session
    }
    override suspend fun clear() {
        saved = null
    }
}
