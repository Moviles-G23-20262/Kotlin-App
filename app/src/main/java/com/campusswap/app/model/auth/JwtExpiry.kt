package com.campusswap.app.data.auth

import com.google.gson.JsonParser
import java.time.Instant
import java.util.Base64

object JwtExpiry {

    fun expiresAt(token: String): Instant? = runCatching {
        val payload = token.split('.')[1]
        val json = String(Base64.getUrlDecoder().decode(payload))
        Instant.ofEpochSecond(JsonParser.parseString(json).asJsonObject.get("exp").asLong)
    }.getOrNull()

    fun isExpired(token: String, now: Instant): Boolean {
        val expiresAt = expiresAt(token) ?: return true
        return !now.isBefore(expiresAt)
    }
}
