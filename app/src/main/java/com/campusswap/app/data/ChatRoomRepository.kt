package com.campusswap.app.data

import com.campusswap.app.data.materials.toProduct
import com.campusswap.app.data.remote.ChatRoomRemoteDataSource
import com.campusswap.app.data.remote.ChatRoomSummaryDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class ChatRoomRepository(private val remote: ChatRoomRemoteDataSource) {
    private val rooms = ConcurrentHashMap<String, String>()
    private val lock = Mutex()

    fun select(productId: String, room: String) {
        rooms[productId] = room
    }

    fun selected(productId: String): String? = rooms[productId]

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
        val all = remote.rooms()
        all.firstOrNull { it.id == rooms[productId] } ?: all.firstOrNull { it.materialId == materialId }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** The listing as the chat room sees it, for conversations about listings that left the feed (reserved, sold). */
    suspend fun productFor(productId: String): Product? {
        val summary = summaryFor(productId) ?: return null
        val product = summary.material?.toProduct() ?: return null
        val sellerName = summary.seller?.fullName ?: return product
        return product.copy(seller = product.seller.copy(name = sellerName))
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
