package com.campusswap.app.model.analytics

import com.campusswap.app.model.remote.AnalyticsEventBatchRequest
import com.campusswap.app.model.remote.AnalyticsEventBatchResult
import com.campusswap.app.model.remote.AnalyticsEventsApi
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class HttpEventSinkTest {
    private class FakeApi : AnalyticsEventsApi {
        val requests = mutableListOf<AnalyticsEventBatchRequest>()
        var failures = ArrayDeque<Exception>()
        override suspend fun createBatch(body: AnalyticsEventBatchRequest): AnalyticsEventBatchResult {
            failures.removeFirstOrNull()?.let { throw it }
            requests += body
            return AnalyticsEventBatchResult(received = body.events.size, created = body.events.size)
        }
    }

    private val api = FakeApi()
    private var signedIn = true
    private val sink = HttpEventSink(api) { signedIn }

    private fun event(name: String, timestamp: Long) =
        AnalyticsEvent(name, timestamp, "session-1", null, "Android 14", "Pixel", mapOf("product_id" to "p1"))

    private fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))

    @Test fun aWholeBatchGoesInOneRequest() = runBlocking {
        val batch = (1L..20L).map { event(Events.SMART_MATCH_SHOWN, it) }

        assertTrue(sink.send(batch))
        assertEquals(1, api.requests.size)
        assertEquals(20, api.requests.single().events.size)
    }

    @Test fun sendsOnlyEventsTheBackendUnderstands() = runBlocking {
        val accepted = sink.send(listOf(event(Events.SCREEN_VIEW, 1), event(Events.SMART_MATCH_SHOWN, 2)))

        assertTrue(accepted)
        assertEquals(listOf("SMART_MATCH_SHOWN"), api.requests.single().events.map { it.eventType })
    }

    @Test fun nothingTheBackendUnderstandsMeansNoRequest() = runBlocking {
        assertTrue(sink.send(listOf(event(Events.SCREEN_VIEW, 1), event(Events.APP_START, 2))))
        assertTrue(api.requests.isEmpty())
    }

    @Test fun offlineKeepsTheBatchAndTheRetryReusesTheSameIds() = runBlocking {
        val batch = listOf(event(Events.SMART_MATCH_SHOWN, 1), event(Events.SMART_MATCH_OPENED, 2))
        api.failures.addLast(IOException("offline"))

        assertFalse(sink.send(batch))
        assertTrue(api.requests.isEmpty())

        assertTrue(sink.send(batch))
        assertTrue(sink.send(batch))
        // The server stores an id once, so sending the same batch again can't duplicate it.
        assertEquals(api.requests[0].events.map { it.clientId }, api.requests[1].events.map { it.clientId })
    }

    @Test fun eventsWithoutASessionAreDropped() = runBlocking {
        signedIn = false

        assertTrue(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))
        assertTrue(api.requests.isEmpty())
    }

    @Test fun aRejectedBatchIsDroppedSoItCannotBlockTheQueue() = runBlocking {
        api.failures.addLast(http(400))

        assertTrue(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))
    }

    @Test fun unauthorizedOrServerErrorKeepsTheBatch() = runBlocking {
        api.failures.addLast(http(401))
        assertFalse(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))

        api.failures.addLast(http(503))
        assertFalse(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))
    }

    @Test fun twoDifferentEventsInTheSameMillisecondGetDifferentIds() = runBlocking {
        val shownP4 = AnalyticsEvent(Events.SMART_MATCH_SHOWN, 5, "session-1", null, "Android 14", "Pixel", mapOf("product_id" to "p4"))
        val shownP6 = shownP4.copy(properties = mapOf("product_id" to "p6"))

        assertTrue(sink.send(listOf(shownP4, shownP6)))
        val ids = api.requests.single().events.map { it.clientId }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
    }
}
