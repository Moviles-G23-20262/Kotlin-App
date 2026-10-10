package com.campusswap.app.data.materials

import com.campusswap.app.data.Product
import com.campusswap.app.data.local.CachedMaterialDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock

/**
 * [isLive] is false when the server couldn't be reached and [products] come from the on-device cache;
 * [updatedAtMillis] says how old that copy is (null when nothing was ever cached).
 */
data class MaterialFeed(val products: List<Product>, val isLive: Boolean, val updatedAtMillis: Long?)

class MaterialRepository(
    private val remote: MaterialRemoteDataSource,
    private val cache: CachedMaterialDao,
    private val clock: Clock,
) {

    suspend fun load(): MaterialFeed = try {
        val available = remote.getMaterials().filter { it.status == "AVAILABLE" }
        val now = clock.millis()
        // Mapping is CPU work, so it runs on Default; Room runs the write on its own I/O executor.
        val (entities, products) = withContext(Dispatchers.Default) {
            available.mapIndexed { i, dto -> dto.toCachedEntity(i, now) } to available.map { it.toProduct() }
        }
        cache.replaceAll(entities)
        MaterialFeed(products, isLive = true, updatedAtMillis = now)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val cached = cache.getAll()
        val products = withContext(Dispatchers.Default) { cached.map { it.toDto().toProduct() } }
        MaterialFeed(products, isLive = false, updatedAtMillis = cached.firstOrNull()?.cachedAtMillis)
    }
}
