package com.campusswap.app.analytics

import com.campusswap.app.data.remote.AnalyticsEventsApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.util.Collections

class HttpEventSink(
    private val api: AnalyticsEventsApi,
    private val hasSession: () -> Boolean,
) : EventSink {
    private val delivered: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override suspend fun send(batch: List<AnalyticsEvent>): Boolean {
        val requests = batch.mapNotNull { event -> BackendEventMapper.toRequest(event)?.let { event to it } }
        if (requests.isEmpty() || !hasSession()) return true

        for ((event, request) in requests) {
            val key = event.toJson().toString()
            if (key in delivered) continue
            try {
                api.create(request)
                delivered += key
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                return false
            } catch (e: HttpException) {
                if (e.code() == 401 || e.code() >= 500) return false
                delivered += key
            }
        }
        return true
    }
}
