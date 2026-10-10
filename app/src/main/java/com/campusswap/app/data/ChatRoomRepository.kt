package com.campusswap.app.data

import com.campusswap.app.data.remote.ChatRoomRemoteDataSource
import com.campusswap.app.data.remote.ChatRoomSummaryDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatRoomRepository(private val remote: ChatRoomRemoteDataSource) {
    private val rooms = mutableMapOf<String, String>()
    private val lock = Mutex()

    /**
     * The room for a listing, from either side. Creating one makes the caller the buyer, and the
     * server refuses that to the seller, so an existing conversation is always looked up first.
     */
    suspend fun roomFor(productId: String): String? = lock.withLock {
        rooms[productId]?.let { return it }
        val materialId = SeedIds.backendMaterial(productId) ?: return null
        val id = remote.rooms().firstOrNull { it.materialId == materialId }?.id ?: remote.open(materialId)
        rooms[productId] = id
        id
    }

    suspend fun summaryFor(productId: String): ChatRoomSummaryDto? = try {
        val materialId = SeedIds.backendMaterial(productId)
        materialId?.let { id -> remote.rooms().firstOrNull { it.materialId == id } }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
