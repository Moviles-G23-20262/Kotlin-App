package com.campusswap.app.data.notifications

import com.campusswap.app.data.AppNotification
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Instant

data class NotificationFeed(
    val items: List<AppNotification>,
    val isLive: Boolean,        // true = llega del backend, false = ultima copia guardada
)

class NotificationRepository(
    private val remote: NotificationRemoteDataSource,
    private val clock: Clock = Clock.systemUTC(),
) {
    private var cachedUserId: String? = null
    private var cached: List<AppNotification> = emptyList()

    suspend fun load(userId: String): NotificationFeed = try {
        val items = remote.getNotifications()
            .filter { it.userId == userId }
            .sortedByDescending { it.sentAt }
            .map { it.toAppNotification() }
        cachedUserId = userId
        cached = items
        NotificationFeed(items, isLive = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sin conexion devuelve lo ultimo que cargamos al usuario
        NotificationFeed(if (cachedUserId == userId) cached else emptyList(), isLive = false)
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