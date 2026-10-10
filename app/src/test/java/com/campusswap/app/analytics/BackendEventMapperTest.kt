package com.campusswap.app.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackendEventMapperTest {
    private fun event(name: String, vararg properties: Pair<String, Any?>) =
        AnalyticsEvent(name, 1_759_420_800_000, "session-1", "alerts", "Android 14", "Pixel", properties.toMap())

    @Test fun smartMatchEventsKeepTheMaterial() {
        val request = BackendEventMapper.toRequest(event(Events.SMART_MATCH_RESERVED, "product_id" to "p1"))!!

        assertEquals("SMART_MATCH_RESERVED", request.eventType)
        assertEquals("b0000000-0000-4000-8000-000000000001", request.materialId)
        assertEquals("smart_match_reserved", request.metadata["event_name"])
        assertEquals("2025-10-02T16:00:00Z", request.occurredAt)
    }

    @Test fun backendMaterialIdsPassThrough() {
        val uuid = "d1000000-0000-4000-8000-000000000007"

        assertEquals(uuid, BackendEventMapper.toRequest(event(Events.SMART_MATCH_SHOWN, "product_id" to uuid))!!.materialId)
    }

    @Test fun locallyPublishedListingsHaveNoMaterial() {
        assertNull(BackendEventMapper.toRequest(event(Events.SMART_MATCH_OPENED, "product_id" to "local-13"))!!.materialId)
    }

    @Test fun wishlistToggleMapsToAddOrRemove() {
        assertEquals("WISHLIST_ADD", BackendEventMapper.toRequest(event(Events.WISHLIST_TOGGLED, "added" to true))!!.eventType)
        assertEquals("WISHLIST_REMOVE", BackendEventMapper.toRequest(event(Events.WISHLIST_TOGGLED, "added" to false))!!.eventType)
    }

    @Test fun exchangeConfirmationKeepsMethodInMetadata() {
        val request = BackendEventMapper.toRequest(event(Events.EXCHANGE_CONFIRMED, "product_id" to "p1", "method" to "gps"))!!

        assertEquals("EXCHANGE_CONFIRMED", request.eventType)
        assertEquals("gps", request.metadata["method"])
        assertNull(request.metadata["product_id"])
    }

    @Test fun eventsWithoutABackendTypeAreNotSent() {
        assertNull(BackendEventMapper.toRequest(event(Events.SCREEN_VIEW)))
        assertNull(BackendEventMapper.toRequest(event(Events.APP_START)))
    }

    @Test fun contactingTheSellerReachesTheBackendForTheBuyerJourney() {
        val request = BackendEventMapper.toRequest(event(Events.CONTACT_SELLER, "product_id" to "p2"))!!

        assertEquals("CONTACT_SELLER", request.eventType)
        assertEquals("b0000000-0000-4000-8000-000000000002", request.materialId)
    }

    @Test fun meetingEventsStayOnTheDeviceUntilTheBackendAcceptsThem() {
        assertNull(BackendEventMapper.toRequest(event(Events.MEETING_PROPOSED, "product_id" to "p1")))
        assertNull(BackendEventMapper.toRequest(event(Events.MEETING_CONFIRMED, "product_id" to "p1")))
    }
}
