package com.campusswap.app.model.analytics

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

    @Test fun sizeCountsWhatIsAlreadyOnDiskAndFollowsAppendsAndDrops() {
        val file = tmp.newFile()
        FileEventStore(file).apply { append(event(1)); append(event(2)) }

        val store = FileEventStore(file)
        assertEquals(2, store.size())
        store.append(event(3))
        assertEquals(3, store.size())
        store.drop(2)
        assertEquals(1, store.size())
        assertEquals(listOf(3L), store.peek(10).map { it.timestamp })
        store.drop(1)
        assertEquals(0, store.size())
        assertTrue(store.peek(10).isEmpty())
    }

    @Test fun peekReadsOnlyWhatItNeeds() {
        val store = FileEventStore(tmp.newFile())
        (1..50).forEach { store.append(event(it)) }

        assertEquals(listOf(1L, 2L, 3L), store.peek(3).map { it.timestamp })
    }
}
