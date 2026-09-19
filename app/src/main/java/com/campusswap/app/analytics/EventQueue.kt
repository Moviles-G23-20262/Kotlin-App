package com.campusswap.app.analytics

import org.json.JSONObject
import java.io.File

/** Disk-backed FIFO (one JSON event per line) so events survive crashes and offline periods. */
class FileEventStore(private val file: File) {
    private val lock = Any()

    fun append(event: AnalyticsEvent) = synchronized(lock) {
        file.appendText(event.toJson().toString() + "\n")
    }

    fun peek(limit: Int): List<AnalyticsEvent> = synchronized(lock) {
        readLines().take(limit).mapNotNull { runCatching { AnalyticsEvent.fromJson(JSONObject(it)) }.getOrNull() }
    }

    fun drop(count: Int) = synchronized(lock) {
        val remaining = readLines().drop(count)
        file.writeText(remaining.joinToString(separator = "") { it + "\n" })
    }

    fun size(): Int = synchronized(lock) { readLines().size }

    private fun readLines(): List<String> =
        if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()
}

/** Batches stored events and hands them to the sink; failed batches stay queued for the next attempt. */
class EventQueue(
    private val store: FileEventStore,
    private val sink: EventSink,
    private val batchSize: Int = 20,
) {
    fun enqueue(event: AnalyticsEvent) = store.append(event)

    fun shouldFlush(): Boolean = store.size() >= batchSize

    /** Sends everything queued, batch by batch. Stops at the first rejected batch. Returns events sent. */
    suspend fun flush(): Int {
        var sent = 0
        while (true) {
            val batch = store.peek(batchSize)
            if (batch.isEmpty()) return sent
            val accepted = runCatching { sink.send(batch) }.getOrDefault(false)
            if (!accepted) return sent
            store.drop(batch.size)
            sent += batch.size
            if (batch.size < batchSize) return sent
        }
    }
}
