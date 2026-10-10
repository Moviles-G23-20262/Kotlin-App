package com.campusswap.app.data

import com.campusswap.app.data.remote.ChatRoomRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatRoomRepository(private val remote: ChatRoomRemoteDataSource) {
    private val rooms = mutableMapOf<String, String>()
    private val lock = Mutex()

    suspend fun roomFor(productId: String): String? = lock.withLock {
        rooms[productId]?.let { return it }
        val materialId = SeedIds.backendMaterial(productId) ?: return null
        remote.open(materialId).also { rooms[productId] = it }
    }

    suspend fun markRead(productId: String) {
        try {
            roomFor(productId)?.let { remote.markRead(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return
        }
    }
}
