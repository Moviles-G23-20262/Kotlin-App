package com.campusswap.app.analytics

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

/**
 * Single entry point every screen and ViewModel logs through. Events are written to disk immediately
 * and shipped to the [EventSink] in batches, so nothing is lost offline or on a crash.
 */
object Analytics {
    private const val FLUSH_INTERVAL_MS = 15_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flushLock = Mutex()
    private val sessionId = UUID.randomUUID().toString()

    private var queue: EventQueue? = null
    private var currentScreen: String? = null

    fun init(context: Context, sink: EventSink? = null) {
        if (queue != null) return
        val dir = File(context.filesDir, "analytics").apply { mkdirs() }
        val store = FileEventStore(File(dir, "pending.jsonl"))
        queue = EventQueue(store, sink ?: LocalSink(File(dir, "sent.jsonl")))
        CrashReporter.install(store) { newEvent(Events.CRASH, it) }
        log(Events.APP_START)
        scope.launch {
            while (true) {
                flush()
                delay(FLUSH_INTERVAL_MS)
            }
        }
    }

    fun screen(name: String) {
        currentScreen = name
        log(Events.SCREEN_VIEW)
    }

    fun log(name: String, vararg properties: Pair<String, Any?>) {
        val q = queue ?: return
        scope.launch {
            q.enqueue(newEvent(name, properties.toMap()))
            if (q.shouldFlush()) flush()
        }
    }

    suspend fun flush(): Int = queue?.let { q -> flushLock.withLock { q.flush() } } ?: 0

    private fun newEvent(name: String, properties: Map<String, Any?>) = AnalyticsEvent(
        name = name,
        timestamp = System.currentTimeMillis(),
        sessionId = sessionId,
        screen = currentScreen,
        osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        properties = properties,
    )
}
