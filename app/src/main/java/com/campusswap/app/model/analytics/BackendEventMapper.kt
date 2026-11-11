package com.campusswap.app.model.analytics

import com.campusswap.app.model.SeedIds
import com.campusswap.app.model.remote.AnalyticsEventBatchItem
import java.time.Instant
import java.util.UUID

object BackendEventMapper {
    private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun toRequest(event: AnalyticsEvent): AnalyticsEventBatchItem? {
        val eventType = backendType(event) ?: return null
        val productId = event.properties["product_id"] as? String
        val createdAt = Instant.ofEpochMilli(event.timestamp).toString()
        return AnalyticsEventBatchItem(
            clientId = clientId(event),
            eventType = eventType,
            materialId = productId?.let { SeedIds.material(it) ?: it.takeIf(UUID_PATTERN::matches) },
            metadata = event.properties - "product_id" - "chat_room_id" + chatRoom(event) + mapOf(
                "event_name" to event.name,
                "session_id" to event.sessionId,
                "screen" to event.screen,
                "os_version" to event.osVersion,
                "device_model" to event.deviceModel,
            ),
            occurredAt = createdAt,
            clientCreatedAt = createdAt,
        )
    }

    /**
     * Same event, same id: built from what identifies the event (session, time, name, properties), so a
     * batch sent again after a lost response or a crash is ignored by the server instead of counted twice.
     */
    internal fun clientId(event: AnalyticsEvent): String =
        UUID.nameUUIDFromBytes(
            "${event.sessionId}|${event.timestamp}|${event.name}|${event.properties.toSortedMap()}".toByteArray(),
        ).toString()

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
        Events.MEETING_CONFIRMED -> "MEETING_CONFIRMED"
        else -> null
    }

    /** The analytics back end (BQ4) reads the room as `chatRoomId`. */
    private fun chatRoom(event: AnalyticsEvent): Map<String, Any?> =
        (event.properties["chat_room_id"] as? String)?.let { mapOf("chatRoomId" to it) }.orEmpty()
}
