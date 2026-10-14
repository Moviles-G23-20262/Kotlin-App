package com.campusswap.app.data.notifications

import com.campusswap.app.data.AppNotification
import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save
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
    private val cache: ResponseCache = ResponseCache.None,
) {
    private var cachedUserId: String? = null
    private var cached: List<AppNotification> = emptyList()

    suspend fun load(userId: String): NotificationFeed = try {
        val mine = remote.getNotifications().filter { it.userId == userId }
        cache.save(userId, KEY, mine)
        val items = toFeed(mine)
        cachedUserId = userId
        cached = items
        NotificationFeed(items, isLive = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sin conexion: lo ultimo de esta sesion, o lo guardado en el telefono por una sesion anterior
        val saved = if (cachedUserId == userId) cached else cache.load<List<NotificationDto>>(userId, KEY)?.value?.let(::toFeed)
        NotificationFeed(saved.orEmpty(), isLive = false)
    }

    private fun toFeed(dtos: List<NotificationDto>) = dtos.sortedByDescending { it.sentAt }.map { it.toAppNotification() }

    suspend fun markOpened(notificationId: String): Boolean = try {
        remote.markOpened(notificationId, Instant.now(clock).toString())
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "notifications"
    }
}
