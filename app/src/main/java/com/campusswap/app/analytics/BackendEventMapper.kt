package com.campusswap.app.analytics

import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.remote.AnalyticsEventRequest
import java.time.Instant

object BackendEventMapper {
    private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun toRequest(event: AnalyticsEvent): AnalyticsEventRequest? {
        val eventType = backendType(event) ?: return null
        val productId = event.properties["product_id"] as? String
        return AnalyticsEventRequest(
            eventType = eventType,
            materialId = productId?.let { SeedIds.material(it) ?: it.takeIf(UUID_PATTERN::matches) },
            metadata = event.properties - "product_id" + mapOf(
                "event_name" to event.name,
                "session_id" to event.sessionId,
                "screen" to event.screen,
                "os_version" to event.osVersion,
                "device_model" to event.deviceModel,
            ),
            occurredAt = Instant.ofEpochMilli(event.timestamp).toString(),
        )
    }

    private fun backendType(event: AnalyticsEvent): String? = when (event.name) {
        Events.LISTING_OPENED -> "LISTING_VIEW"
        Events.CONTACT_SELLER -> "CONTACT_SELLER"
        Events.SEARCH_PERFORMED -> "SEARCH"
        Events.WISHLIST_TOGGLED -> if (event.properties["added"] == true) "WISHLIST_ADD" else "WISHLIST_REMOVE"
        Events.MATCH_NOTIFIED -> "NOTIFICATION_SENT"
        Events.SMART_MATCH_SHOWN -> "SMART_MATCH_SHOWN"
        Events.SMART_MATCH_OPENED -> "SMART_MATCH_OPENED"
        Events.SMART_MATCH_RESERVED -> "SMART_MATCH_RESERVED"
        Events.EXCHANGE_CONFIRMED -> "EXCHANGE_CONFIRMED"
        else -> null
    }
}
