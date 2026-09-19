package com.campusswap.app.analytics

import org.json.JSONObject

data class AnalyticsEvent(
    val name: String,
    val timestamp: Long,
    val sessionId: String,
    val screen: String?,
    val osVersion: String,
    val deviceModel: String,
    val properties: Map<String, Any?> = emptyMap(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("timestamp", timestamp)
        put("sessionId", sessionId)
        put("screen", screen ?: JSONObject.NULL)
        put("osVersion", osVersion)
        put("deviceModel", deviceModel)
        put("properties", JSONObject().also { p -> properties.forEach { (k, v) -> p.put(k, v ?: JSONObject.NULL) } })
    }

    companion object {
        fun fromJson(json: JSONObject): AnalyticsEvent {
            val props = json.optJSONObject("properties")
            return AnalyticsEvent(
                name = json.getString("name"),
                timestamp = json.getLong("timestamp"),
                sessionId = json.getString("sessionId"),
                screen = if (json.isNull("screen")) null else json.getString("screen"),
                osVersion = json.getString("osVersion"),
                deviceModel = json.getString("deviceModel"),
                properties = props?.keys()?.asSequence()?.associateWith { props.opt(it).takeIf { v -> v != JSONObject.NULL } } ?: emptyMap(),
            )
        }
    }
}

/** Event names, grouped by the Business Question they feed. */
object Events {
    const val APP_START = "app_start"
    const val SCREEN_VIEW = "screen_view"
    const val CRASH = "crash"                                   // BQ1
    const val LISTING_OPENED = "listing_opened"                 // BQ2, BQ6
    const val CHAT_MESSAGE_SENT = "chat_message_sent"           // BQ4
    const val MEETING_PROPOSED = "meeting_proposed"             // BQ4
    const val MEETING_CONFIRMED = "meeting_confirmed"           // BQ4
    const val LISTING_PUBLISHED = "listing_published"           // BQ3, BQ6, BQ8, BQ9
    const val WISHLIST_TOGGLED = "wishlist_toggled"             // BQ7
    const val ALERT_CREATED = "alert_created"                   // BQ7
    const val MATCH_NOTIFIED = "match_notified"                 // BQ7
    const val CART_ADD = "cart_add"
    const val PURCHASE_COMPLETED = "purchase_completed"         // BQ2, BQ6, BQ7
    const val EXCHANGE_RATED = "exchange_rated"
    const val SEARCH_PERFORMED = "search_performed"            // BQ5
    const val LOGIN_SUCCESS = "login_success"
}
