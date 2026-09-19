package com.campusswap.app.analytics

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EventQueueTest {
    @get:Rule val tmp = TemporaryFolder()

    private class FakeSink(var online: Boolean) : EventSink {
        val received = mutableListOf<AnalyticsEvent>()
        override suspend fun send(batch: List<AnalyticsEvent>): Boolean {
            if (!online) return false
            received += batch
            return true
        }
    }

    private fun event(i: Int) = AnalyticsEvent(
        "e$i", i.toLong(), "s", "home", "Android 14", "Pixel", mapOf("n" to i, "empty" to null),
    )

    @Test fun offlineEventsStayQueuedThenFlushInOrder() = runBlocking {
        val sink = FakeSink(online = false)
        val queue = EventQueue(FileEventStore(tmp.newFile()), sink, batchSize = 2)
        (1..5).forEach { queue.enqueue(event(it)) }

        assertEquals(0, queue.flush())
        assertTrue(sink.received.isEmpty())

        sink.online = true
        assertEquals(5, queue.flush())
        assertEquals(listOf("e1", "e2", "e3", "e4", "e5"), sink.received.map { it.name })
        assertEquals(0, queue.flush())
    }

    @Test fun eventsSurviveRestartAndRoundTripFields() = runBlocking {
        val file = tmp.newFile()
        FileEventStore(file).append(event(7))
        val sink = FakeSink(online = true)
        EventQueue(FileEventStore(file), sink).flush()

        val got = sink.received.single()
        assertEquals("e7", got.name)
        assertEquals("home", got.screen)
        assertEquals(7, got.properties["n"])
        assertEquals(null, got.properties["empty"])
    }

    @Test fun shouldFlushOnceBatchSizeReached() {
        val queue = EventQueue(FileEventStore(tmp.newFile()), FakeSink(true), batchSize = 3)
        queue.enqueue(event(1)); queue.enqueue(event(2))
        assertTrue(!queue.shouldFlush())
        queue.enqueue(event(3))
        assertTrue(queue.shouldFlush())
    }
}
