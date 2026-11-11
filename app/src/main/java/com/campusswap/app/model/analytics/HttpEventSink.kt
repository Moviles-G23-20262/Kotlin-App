package com.campusswap.app.model.analytics

import com.campusswap.app.model.remote.AnalyticsEventBatchRequest
import com.campusswap.app.model.remote.AnalyticsEventsApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Sends a batch of the queue in a single `POST /analytics-events/batch` instead of one request per
 * event. The server ignores an event it already has (same client id), so a whole batch can be resent
 * after a lost response without duplicating anything, and one invalid event is skipped by the server
 * without failing the rest.
 */
class HttpEventSink(
    private val api: AnalyticsEventsApi,
    private val hasSession: () -> Boolean,
) : EventSink {
    override suspend fun send(batch: List<AnalyticsEvent>): Boolean {
        val events = batch.mapNotNull(BackendEventMapper::toRequest)
        if (events.isEmpty() || !hasSession()) return true

        return try {
            api.createBatch(AnalyticsEventBatchRequest(events))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            false
        } catch (e: HttpException) {
            // 401 (session expired) and 5xx keep the batch for later; any other 4xx would fail forever.
            e.code() != 401 && e.code() < 500
        }
    }
}
