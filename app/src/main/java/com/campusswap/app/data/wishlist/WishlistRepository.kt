package com.campusswap.app.data.wishlist

import com.campusswap.app.data.Product
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save
import com.campusswap.app.data.materials.toProduct
import com.campusswap.app.data.sync.isRetryable
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Lo que pasó al guardar o quitar un favorito en el backend. */
enum class WishlistResult { SAVED, OFFLINE, NOT_ON_SERVER, REFUSED }

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

    /** Guarda el favorito en el backend; [WishlistResult.OFFLINE] le dice al llamador que lo encole. */
    suspend fun add(materialId: String): WishlistResult {
        // Una publicación pendiente del Outbox ("pending-1") todavía no existe en el servidor
        if (!SeedIds.isBackendId(materialId)) return WishlistResult.NOT_ON_SERVER
        return try {
            itemIds[materialId] = remote.add(materialId).id
            WishlistResult.SAVED
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // 409: ya estaba guardado (un reintento que llegó dos veces); el id se busca al quitarlo
            if (e.code() == 409) WishlistResult.SAVED else failure(e)
        } catch (e: Exception) {
            failure(e)
        }
    }

    /** Quita el favorito del backend; si ya no estaba allá también cuenta como hecho. */
    suspend fun remove(materialId: String): WishlistResult {
        if (!SeedIds.isBackendId(materialId)) return WishlistResult.SAVED
        return try {
            // Desde el Outbox (app recién abierta) el id del item todavía no se conoce: se busca
            val itemId = itemIds[materialId] ?: remote.getAll().firstOrNull { it.materialId == materialId }?.id
            if (itemId != null) remote.remove(itemId)
            itemIds.remove(materialId)
            WishlistResult.SAVED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failure(e)
        }
    }

    /** Sin red o servidor caído vale la pena reintentar; cualquier otro error es un "no" del backend. */
    private fun failure(e: Exception): WishlistResult = when {
        e is IOException -> WishlistResult.OFFLINE
        e is HttpException && isRetryable(e.code()) -> WishlistResult.OFFLINE
        else -> WishlistResult.REFUSED
    }

    /** Al cerrar sesion el siguiente usuario no heredar los ids del anterior */
    fun clear() = itemIds.clear()

    private companion object {
        const val KEY = "wishlist"
    }
}
