package com.campusswap.app.model.analytics

import com.campusswap.app.views.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class CrashTelemetryTest {
    @Test
    fun `screen name drops the route arguments`() {
        assertEquals("chat", CrashTelemetry.screenName(Routes.CHAT))
        assertEquals("product", CrashTelemetry.screenName(Routes.PRODUCT_DETAIL))
        assertEquals("forgot-password", CrashTelemetry.screenName(Routes.FORGOT_PASSWORD))
        assertEquals("home", CrashTelemetry.screenName(Routes.HOME))
    }

    @Test
    fun `every route belongs to a feature`() {
        val routes = listOf(
            Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD, Routes.VERIFY_EMAIL, Routes.HOME,
            Routes.SEARCH, Routes.SELL, Routes.CART, Routes.CHECKOUT, Routes.CONFIRMATION, Routes.PROFILE,
            Routes.PRODUCT_DETAIL, Routes.NOTIFICATIONS, Routes.WISHLIST, Routes.ALERTS, Routes.MESSAGES,
            Routes.CHAT, Routes.MEETING_POINT, Routes.COMPLETION,
        )
        routes.forEach { route -> assert(CrashTelemetry.featureOf(route) != "other") { "$route has no feature" } }
    }

    @Test
    fun `screens of the same flow share a feature`() {
        assertEquals("checkout", CrashTelemetry.featureOf(Routes.CART))
        assertEquals("checkout", CrashTelemetry.featureOf(Routes.CONFIRMATION))
        assertEquals("chat", CrashTelemetry.featureOf(Routes.MESSAGES))
        assertEquals("chat", CrashTelemetry.featureOf(Routes.CHAT))
    }
}
