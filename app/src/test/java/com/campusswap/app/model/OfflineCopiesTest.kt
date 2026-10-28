package com.campusswap.app.data

import com.campusswap.app.data.local.InMemoryResponseCache
import com.campusswap.app.data.materials.MaterialDto
import com.campusswap.app.data.notifications.MaterialSummaryDto
import com.campusswap.app.data.notifications.NotificationDto
import com.campusswap.app.data.notifications.NotificationRemoteDataSource
import com.campusswap.app.data.notifications.NotificationRepository
import com.campusswap.app.data.wishlist.WishlistItemDto
import com.campusswap.app.data.wishlist.WishlistRemoteDataSource
import com.campusswap.app.data.wishlist.WishlistRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/** Wishlist and notifications survive an app restart without a connection. */
class OfflineCopiesTest {
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val cache = InMemoryResponseCache()

    private class FakeNotifications(var items: List<NotificationDto>) : NotificationRemoteDataSource {
        var failure: Exception? = null
        override suspend fun getNotifications(): List<NotificationDto> = failure?.let { throw it } ?: items
        override suspend fun markOpened(id: String, openedAt: String) = Unit
    }

    private class FakeWishlist(var items: List<WishlistItemDto>) : WishlistRemoteDataSource {
        var failure: Exception? = null
        override suspend fun getAll(): List<WishlistItemDto> = failure?.let { throw it } ?: items
        override suspend fun add(materialId: String) = error("not used")
        override suspend fun remove(itemId: String) = Unit
    }

    private fun notification(id: String, userId: String, sentAt: String) =
        NotificationDto(id, userId, "m1", "SMART_MATCH", sentAt, null, MaterialSummaryDto("m1", "Casio fx-991"))

    @Test fun notificationsSavedInAnEarlierRunAreShownOfflineNewestFirst() = runTest {
        val remote = FakeNotifications(
            listOf(
                notification("n1", me, "2026-10-05T10:00:00Z"),
                notification("n2", me, "2026-10-06T10:00:00Z"),
                notification("other", "someone-else", "2026-10-07T10:00:00Z"),
            ),
        )
        NotificationRepository(remote, cache = cache).load(me)

        remote.failure = IOException("airplane mode")
        val afterRestart = NotificationRepository(remote, cache = cache).load(me)

        assertFalse(afterRestart.isLive)
        assertEquals(listOf("n2", "n1"), afterRestart.items.map { it.id })
        assertEquals("Casio fx-991 matches one of your alerts", afterRestart.items.first().message)
    }

    @Test fun theWishlistSavedInAnEarlierRunIsShownOffline() = runTest {
        val material = MaterialDto("d1000000-0000-4000-8000-000000000007", "Arduino UNO", "", null, "45000", "GOOD", "AVAILABLE", "LAB_EQUIPMENT", "s1")
        val remote = FakeWishlist(listOf(WishlistItemDto("w1", me, material.id, material, "2026-10-05T10:00:00Z")))
        WishlistRepository(remote, { me }, cache).load()

        remote.failure = IOException("airplane mode")

        assertEquals(listOf("Arduino UNO"), WishlistRepository(remote, { me }, cache).load()?.map { it.title })
        assertNull(WishlistRepository(remote, { "someone-else" }, cache).load())
    }
}
