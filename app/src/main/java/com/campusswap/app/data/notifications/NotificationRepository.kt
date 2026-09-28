package com.campusswap.app.data.notifications

import com.campusswap.app.data.AppNotification
import com.campusswap.app.data.SampleData
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Instant

data class NotificationFeed(
    val items: List<AppNotification>,
    val isLive: Boolean,        // true = llegan del backend, false = son de SampleData
)

class NotificationRepository(
    private val remote: NotificationRemoteDataSource,
    private val clock: Clock = Clock.systemUTC(),
    private val offlineFallback: () -> List<AppNotification> = { SampleData.notifications },
) {
    suspend fun load(userId: String): NotificationFeed = try {
        val items = remote.getNotifications()
            .filter { it.userId == userId }
            .sortedByDescending { it.sentAt }
            .map { it.toAppNotification() }
        NotificationFeed(items, isLive = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NotificationFeed(offlineFallback(), isLive = false)     // si algo falla, se devuelven notificaciones de ejemplo
    }

    suspend fun markOpened(notificationId: String): Boolean = try {
        remote.markOpened(notificationId, Instant.now(clock).toString())
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }
}
