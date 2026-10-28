package com.campusswap.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JwtExpiryTest {
    private val now = Instant.parse("2026-10-02T15:00:00Z")

    @Test fun readsTheExpiryClaim() {
        val expiresAt = Instant.parse("2026-10-09T15:00:00Z")

        assertEquals(expiresAt, JwtExpiry.expiresAt(jwtExpiringAt(expiresAt)))
    }

    @Test fun tokenIsValidBeforeItsExpiry() {
        assertFalse(JwtExpiry.isExpired(jwtExpiringAt(now.plusSeconds(60)), now))
    }

    @Test fun tokenIsExpiredAtOrAfterItsExpiry() {
        assertTrue(JwtExpiry.isExpired(jwtExpiringAt(now), now))
        assertTrue(JwtExpiry.isExpired(jwtExpiringAt(now.minusSeconds(1)), now))
    }

    @Test fun malformedTokenCountsAsExpired() {
        assertTrue(JwtExpiry.isExpired("not-a-jwt", now))
        assertTrue(JwtExpiry.isExpired("a.%%%.c", now))
    }
}
