package com.campusswap.app.model.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock

/** A copy of what the server answered, and when, for a screen to show while offline. */
data class Saved<T>(val value: T, val savedAtMillis: Long)

/**
 * Keeps the last server answer of a screen (chat thread, inbox...) on the phone. Repositories
 * write it on every successful load and read it back when the network fails.
 */
interface ResponseCache {
    suspend fun put(ownerId: String, key: String, json: String)
    suspend fun get(ownerId: String, key: String): Saved<String>?

    companion object {
        /** Owner for data that is the same for every account, like the campus meeting points. */
        const val SHARED = "*"
    }

    /** For tests and code paths that don't need offline copies. */
    object None : ResponseCache {
        override suspend fun put(ownerId: String, key: String, json: String) = Unit
        override suspend fun get(ownerId: String, key: String): Saved<String>? = null
    }
}

class RoomResponseCache(private val dao: ResponseCacheDao, private val clock: Clock) : ResponseCache {
    override suspend fun put(ownerId: String, key: String, json: String) =
        dao.put(CachedResponseEntity(ownerId, key, json, clock.millis()))

    override suspend fun get(ownerId: String, key: String): Saved<String>? =
        dao.get(ownerId, key)?.let { Saved(it.json, it.cachedAtMillis) }
}

@PublishedApi internal val cacheGson = Gson()

/** JSON work runs on Default so a long thread never blocks the main thread. */
suspend inline fun <reified T> ResponseCache.save(ownerId: String, key: String, value: T) {
    if (this === ResponseCache.None) return
    put(ownerId, key, withContext(Dispatchers.Default) { cacheGson.toJson(value) })
}

suspend inline fun <reified T> ResponseCache.load(ownerId: String, key: String): Saved<T>? {
    val saved = get(ownerId, key) ?: return null
    val value = withContext(Dispatchers.Default) {
        runCatching { cacheGson.fromJson<T>(saved.value, object : TypeToken<T>() {}.type) }.getOrNull()
    } ?: return null
    return Saved(value, saved.savedAtMillis)
}
