package com.campusswap.app.model.sellers

import android.util.LruCache
import com.campusswap.app.model.Seller
import com.campusswap.app.model.local.SellerProfileDao
import com.campusswap.app.model.local.SellerProfileEntity
import com.campusswap.app.model.materials.MaterialDto
import com.campusswap.app.model.ratings.RatingDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Clock

sealed interface SellerProfileLoad {
    /** Just loaded from the server (or from memory, loaded less than [FRESH_MILLIS] ago). */
    data class Live(val profile: SellerProfile) : SellerProfileLoad

    /** No connection: the copy saved on the phone, and when it was saved. */
    data class Saved(val profile: SellerProfile, val savedAtMillis: Long) : SellerProfileLoad

    /** No connection and this seller was never opened on this phone. */
    data object Unavailable : SellerProfileLoad
}

class SellerProfileRepository(
    private val api: SellerProfileApi,
    private val dao: SellerProfileDao,
    private val clock: Clock,
) {
    private data class Entry(val profile: SellerProfile, val savedAtMillis: Long)

    private val memory = LruCache<String, Entry>(MEMORY_ENTRIES)
    private val gson = Gson()

    /** Memory first, then Room. Never uses the network: the screen shows this while the server answers. */
    suspend fun saved(sellerId: String, fallback: Seller?): SellerProfileLoad.Saved? {
        memory.get(sellerId)?.let { return SellerProfileLoad.Saved(it.profile, it.savedAtMillis) }
        val row = dao.get(sellerId) ?: return null
        val profile = withContext(Dispatchers.Default) {
            buildSellerProfile(
                sellerId,
                gson.fromJson<List<RatingDto>>(row.ratingsJson, RATINGS_TYPE),
                gson.fromJson<List<MaterialDto>>(row.listingsJson, LISTINGS_TYPE),
                fallback,
            )
        }
        memory.put(sellerId, Entry(profile, row.savedAtMillis))
        return SellerProfileLoad.Saved(profile, row.savedAtMillis)
    }

    /** skips the memory cache (pull to refresh). Without a connection it falls back to the saved copy. */
    suspend fun load(sellerId: String, fallback: Seller?, force: Boolean = false): SellerProfileLoad {
        if (!force) {
            memory.get(sellerId)
                ?.takeIf { clock.millis() - it.savedAtMillis < FRESH_MILLIS }
                ?.let { return SellerProfileLoad.Live(it.profile) }
        }
        return try {
            // Both requests run at the same time: the wait is the slowest one, not the sum of both.
            val (ratings, listings) = coroutineScope {
                val ratings = async { api.ratingsFor(sellerId) }
                val listings = async { api.listingsBy(sellerId) }
                ratings.await() to listings.await()
            }
            val now = clock.millis()
            val (profile, row) = withContext(Dispatchers.Default) {
                buildSellerProfile(sellerId, ratings, listings, fallback) to
                    SellerProfileEntity(sellerId, gson.toJson(ratings), gson.toJson(listings), now)
            }
            memory.put(sellerId, Entry(profile, now))
            dao.save(row, KEEP_ON_DISK)
            SellerProfileLoad.Live(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            saved(sellerId, fallback) ?: SellerProfileLoad.Unavailable
        }
    }

    private companion object {
        const val MEMORY_ENTRIES = 20
        const val KEEP_ON_DISK = 30
        const val FRESH_MILLIS = 2 * 60 * 1000L
        val RATINGS_TYPE = object : TypeToken<List<RatingDto>>() {}.type
        val LISTINGS_TYPE = object : TypeToken<List<MaterialDto>>() {}.type
    }
}