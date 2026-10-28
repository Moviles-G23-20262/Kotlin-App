package com.campusswap.app.analytics

import com.campusswap.app.data.remote.AnalyticsEventRequest
import com.campusswap.app.data.remote.AnalyticsEventsApi
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class HttpEventSinkTest {
    private class FakeApi : AnalyticsEventsApi {
        val sent = mutableListOf<AnalyticsEventRequest>()
        var failures = ArrayDeque<Exception>()
        override suspend fun create(body: AnalyticsEventRequest) {
            failures.removeFirstOrNull()?.let { throw it }
            sent += body
        }
    }

    private val api = FakeApi()
    private var signedIn = true
    private val sink = HttpEventSink(api) { signedIn }

    private fun event(name: String, timestamp: Long) =
        AnalyticsEvent(name, timestamp, "session-1", null, "Android 14", "Pixel", mapOf("product_id" to "p1"))

    private fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))

    @Test fun sendsOnlyEventsTheBackendUnderstands() = runBlocking {
        val accepted = sink.send(listOf(event(Events.SCREEN_VIEW, 1), event(Events.SMART_MATCH_SHOWN, 2)))

        assertTrue(accepted)
        assertEquals(listOf("SMART_MATCH_SHOWN"), api.sent.map { it.eventType })
    }

    @Test fun offlineKeepsTheBatchAndARetryDoesNotDuplicate() = runBlocking {
        val batch = listOf(event(Events.SMART_MATCH_SHOWN, 1), event(Events.SMART_MATCH_OPENED, 2))
        api.failures.addLast(IOException("offline"))

        assertFalse(sink.send(batch))
        assertTrue(api.sent.isEmpty())

        assertTrue(sink.send(batch))
        assertEquals(listOf("SMART_MATCH_SHOWN", "SMART_MATCH_OPENED"), api.sent.map { it.eventType })

        assertTrue(sink.send(batch))
        assertEquals(2, api.sent.size)
    }

    @Test fun partialFailureResendsOnlyWhatWasNotDelivered() = runBlocking {
        val batch = listOf(event(Events.SMART_MATCH_SHOWN, 1), event(Events.SMART_MATCH_OPENED, 2))
        val flaky = object : AnalyticsEventsApi {
            var calls = 0
            val sent = mutableListOf<String>()
            override suspend fun create(body: AnalyticsEventRequest) {
                calls++
                if (calls == 2) throw IOException("dropped")
                sent += body.eventType
            }
        }
        val flakySink = HttpEventSink(flaky) { true }

        assertFalse(flakySink.send(batch))
        assertTrue(flakySink.send(batch))
        assertEquals(listOf("SMART_MATCH_SHOWN", "SMART_MATCH_OPENED"), flaky.sent)
    }

    @Test fun eventsWithoutASessionAreDropped() = runBlocking {
        signedIn = false

        assertTrue(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))
        assertTrue(api.sent.isEmpty())
    }

    @Test fun invalidEventIsSkippedSoItCannotBlockTheQueue() = runBlocking {
        api.failures.addLast(http(400))

        assertTrue(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1), event(Events.SMART_MATCH_OPENED, 2))))
        assertEquals(listOf("SMART_MATCH_OPENED"), api.sent.map { it.eventType })
    }

    @Test fun unauthorizedOrServerErrorKeepsTheBatch() = runBlocking {
        api.failures.addLast(http(401))
        assertFalse(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))

        api.failures.addLast(http(503))
        assertFalse(sink.send(listOf(event(Events.SMART_MATCH_SHOWN, 1))))
    }

    @Test fun twoDifferentEventsInTheSameMillisecondAreBothSent() = runBlocking {
        val shownP4 = AnalyticsEvent(Events.SMART_MATCH_SHOWN, 5, "session-1", null, "Android 14", "Pixel", mapOf("product_id" to "p4"))
        val shownP6 = shownP4.copy(properties = mapOf("product_id" to "p6"))

        assertTrue(sink.send(listOf(shownP4, shownP6)))
        assertEquals(2, api.sent.size)
    }
}
