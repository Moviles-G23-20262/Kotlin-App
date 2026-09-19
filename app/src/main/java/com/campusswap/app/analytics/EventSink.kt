package com.campusswap.app.analytics

import android.util.Log
import java.io.File

/** Where batches of events go. The backend repo will provide an HTTP implementation. */
interface EventSink {
    /** Returns true only if the whole batch was accepted; the queue keeps it otherwise. */
    suspend fun send(batch: List<AnalyticsEvent>): Boolean
}

/** Stand-in until the backend exists: logs each event and archives it to a local file as evidence. */
class LocalSink(private val archive: File) : EventSink {
    override suspend fun send(batch: List<AnalyticsEvent>): Boolean {
        batch.forEach { Log.i(TAG, it.toJson().toString()) }
        archive.appendText(batch.joinToString(separator = "") { it.toJson().toString() + "\n" })
        return true
    }

    companion object {
        const val TAG = "CampusSwapAnalytics"
    }
}
