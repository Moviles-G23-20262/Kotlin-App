package com.campusswap.app.data.local

/** [ResponseCache] that lives in a map, like Room would between two app runs. */
class InMemoryResponseCache(var now: Long = 1_000L) : ResponseCache {
    private val entries = mutableMapOf<Pair<String, String>, Saved<String>>()

    override suspend fun put(ownerId: String, key: String, json: String) {
        entries[ownerId to key] = Saved(json, now)
    }

    override suspend fun get(ownerId: String, key: String): Saved<String>? = entries[ownerId to key]
}
