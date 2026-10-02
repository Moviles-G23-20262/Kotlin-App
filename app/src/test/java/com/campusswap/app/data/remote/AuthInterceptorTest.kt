package com.campusswap.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private val backend = MockWebServer()
    private val otherHost = MockWebServer()
    private var token: String? = "jwt-123"
    private var unauthorizedCalls = 0

    @Before fun start() {
        backend.start()
        otherHost.start()
    }

    @After fun stop() {
        backend.shutdown()
        otherHost.shutdown()
    }

    private fun client() = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(backend.url("/"), { token }, { unauthorizedCalls++ }))
        .build()

    private fun call(server: MockWebServer, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code))
        client().newCall(Request.Builder().url(server.url("/meeting-points")).build()).execute().close()
    }

    @Test fun sendsTheTokenToTheBackend() {
        call(backend)

        assertEquals("Bearer jwt-123", backend.takeRequest().getHeader("Authorization"))
    }

    @Test fun neverSendsTheTokenToAnotherHost() {
        call(otherHost)

        assertNull(otherHost.takeRequest().getHeader("Authorization"))
    }

    @Test fun sendsNoHeaderWithoutASession() {
        token = null

        call(backend)

        assertNull(backend.takeRequest().getHeader("Authorization"))
    }

    @Test fun unauthorizedResponseEndsTheSession() {
        call(backend, code = 401)

        assertEquals(1, unauthorizedCalls)
    }

    @Test fun unauthorizedWithoutATokenIsNotAnExpiry() {
        token = null

        call(backend, code = 401)

        assertEquals(0, unauthorizedCalls)
    }
}
