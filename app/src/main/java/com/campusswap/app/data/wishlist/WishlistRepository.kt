package com.campusswap.app.data.wishlist

import com.campusswap.app.data.Product
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.materials.toProduct
import kotlinx.coroutines.CancellationException

class WishlistRepository(private val remote: WishlistRemoteDataSource) {

    /**id del WishlistItem el DELETE usa el id del item, no el del material. */
    private val itemIds = mutableMapOf<String, String>()

    suspend fun load(): List<Product>? = try {
        val items = remote.getAll().sortedByDescending { it.createdAt }
        itemIds.clear()
        items.forEach { itemIds[it.materialId] = it.id }
        items.mapNotNull { it.material?.toProduct() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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
}