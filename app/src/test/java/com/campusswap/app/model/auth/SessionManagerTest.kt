package com.campusswap.app.data.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {
    private val now = Instant.parse("2026-10-02T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val validSession = Session(jwtExpiringAt(now.plusSeconds(3600)), "user-1", "ana@uniandes.edu.co", "Ana")
    private val expiredSession = validSession.copy(token = jwtExpiringAt(now.minusSeconds(1)))

    private fun manager(store: SessionStore, scope: kotlinx.coroutines.CoroutineScope) = SessionManager(store, clock, scope)

    @Test fun startsRestoringUntilTheStoreIsRead() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(SessionState.Restoring, manager(FakeSessionStore(), this).state.value)
    }

    @Test fun restoresAValidSavedSession() = runTest(UnconfinedTestDispatcher()) {
        val sessions = manager(FakeSessionStore(validSession), this)

        sessions.restore()

        assertEquals(SessionState.SignedIn(validSession), sessions.state.value)
        assertEquals(validSession.token, sessions.token)
    }

    @Test fun discardsAnExpiredSavedSession() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore(expiredSession)
        val sessions = manager(store, this)

        sessions.restore()

        assertEquals(SessionState.SignedOut(expired = false), sessions.state.value)
        assertNull(store.saved)
        assertNull(sessions.token)
    }

    @Test fun startingASessionPersistsIt() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val sessions = manager(store, this)

        sessions.start(validSession)

        assertEquals(validSession, store.saved)
        assertEquals(SessionState.SignedIn(validSession), sessions.state.value)
    }

    @Test fun signOutClearsTokenAndStore() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val sessions = manager(store, this)
        sessions.start(validSession)

        sessions.signOut()

        assertEquals(SessionState.SignedOut(expired = false), sessions.state.value)
        assertNull(store.saved)
        assertNull(sessions.token)
    }

    @Test fun expiryIsReportedSoTheUserCanBeTold() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeSessionStore()
        val sessions = manager(store, this)
        sessions.start(validSession)

        sessions.expire()

        assertEquals(SessionState.SignedOut(expired = true), sessions.state.value)
        assertNull(store.saved)
    }

    @Test fun validSessionRejectsATokenThatExpiredWhileInUse() = runTest(UnconfinedTestDispatcher()) {
        val sessions = manager(FakeSessionStore(), this)
        sessions.start(expiredSession)

        assertNull(sessions.validSession())
        assertEquals(SessionState.SignedOut(expired = true), sessions.state.value)
    }
}
