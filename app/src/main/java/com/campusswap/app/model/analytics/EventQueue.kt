package com.campusswap.app.model.analytics

import androidx.tracing.trace
import org.json.JSONObject
import java.io.File

/**
 * Disk-backed FIFO (one JSON event per line) so events survive crashes and offline periods.
 *
 * The number of queued lines is counted from the file once and then kept in memory. Before, every
 * [size] call (one per logged event, through [EventQueue.shouldFlush]) read and split the whole file,
 * so logging got slower the longer the phone stayed offline.
 */
class FileEventStore(private val file: File) {
    private val lock = Any()

    /** Lines in [file]; -1 until the first time it is needed. */
    private var count = -1

    fun append(event: AnalyticsEvent) = synchronized(lock) {
        file.appendText(event.toJson().toString() + "\n")
        if (count >= 0) count++
    }

    /** Reads only the first [limit] lines instead of the whole file. */
    fun peek(limit: Int): List<AnalyticsEvent> = synchronized(lock) {
        if (count == 0 || !file.exists()) return@synchronized emptyList()
        file.useLines { lines -> lines.filter { it.isNotBlank() }.take(limit).toList() }
            .mapNotNull { runCatching { AnalyticsEvent.fromJson(JSONObject(it)) }.getOrNull() }
    }

    fun drop(count: Int) = synchronized(lock) {
        val remaining = readLines().drop(count)
        file.writeText(remaining.joinToString(separator = "") { it + "\n" })
        this.count = remaining.size
    }

    fun size(): Int = synchronized(lock) {
        if (count < 0) count = readLines().size
        count
    }

    private fun readLines(): List<String> =
        if (file.exists()) file.readLines().filter { it.isNotBlank() } else emptyList()
}

/** Batches stored events and hands them to the sink; failed batches stay queued for the next attempt. */
class EventQueue(
    private val store: FileEventStore,
    private val sink: EventSink,
    private val batchSize: Int = 20,
) {
    fun enqueue(event: AnalyticsEvent) = trace("EventQueue.enqueue") { store.append(event) }

    fun shouldFlush(): Boolean = trace("EventQueue.shouldFlush") { store.size() >= batchSize }

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
