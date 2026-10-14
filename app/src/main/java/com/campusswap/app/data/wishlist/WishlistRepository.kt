package com.campusswap.app.data.wishlist

import com.campusswap.app.data.Product
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save
import com.campusswap.app.data.materials.toProduct
import kotlinx.coroutines.CancellationException

class WishlistRepository(
    private val remote: WishlistRemoteDataSource,
    private val currentUserId: () -> String? = { null },
    private val cache: ResponseCache = ResponseCache.None,
) {

    /**id del WishlistItem el DELETE usa el id del item, no el del material. */
    private val itemIds = mutableMapOf<String, String>()

    /** Offline it returns the list saved by the last load; null only if there is none. */
    suspend fun load(): List<Product>? = try {
        val items = remote.getAll().sortedByDescending { it.createdAt }
        currentUserId()?.let { cache.save(it, KEY, items) }
        show(items)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentUserId()?.let { cache.load<List<WishlistItemDto>>(it, KEY) }?.let { show(it.value) }
    }

    private fun show(items: List<WishlistItemDto>): List<Product> {
        itemIds.clear()
        items.forEach { itemIds[it.materialId] = it.id }
        return items.mapNotNull { it.material?.toProduct() }
    }

    /** true si el backend guarda */
    suspend fun add(materialId: String): Boolean {
        // Una publicación pendiente del Outbox ("pending-1") todavía no existe en el servidor
        if (!SeedIds.isBackendId(materialId)) return false
        return try {
            itemIds[materialId] = remote.add(materialId).id
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** true si el backend lo quita */
    suspend fun remove(materialId: String): Boolean {
        val itemId = itemIds[materialId] ?: return true
        return try {
            remote.remove(itemId)
            itemIds.remove(materialId)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /** Al cerrar sesion el siguiente usuario no heredar los ids del anterior */
    fun clear() = itemIds.clear()

    private companion object {
        const val KEY = "wishlist"
    }
}
