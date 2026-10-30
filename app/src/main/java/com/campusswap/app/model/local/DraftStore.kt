package com.campusswap.app.model.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campusswap.app.model.SellDraft
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

/**
 * Keeps the listing being written, so closing the app or losing signal mid-form doesn't lose it.
 * Each account has its own draft ([userId] null = demo access), so a new log-in never sees someone else's.
 */
interface DraftStore {
    suspend fun read(userId: String?): SellDraft?
    suspend fun save(userId: String?, draft: SellDraft)
    suspend fun clear(userId: String?)
}

private val Context.draftDataStore by preferencesDataStore(name = "sell_draft")

class DataStoreDraftStore(private val context: Context, private val gson: Gson = Gson()) : DraftStore {

    override suspend fun read(userId: String?): SellDraft? {
        val json = context.draftDataStore.data.first()[key(userId)] ?: return null
        // A draft saved by an older app version may not parse; starting fresh is better than crashing.
        return runCatching { gson.fromJson(json, SellDraft::class.java) }.getOrNull()
    }

    override suspend fun save(userId: String?, draft: SellDraft) {
        context.draftDataStore.edit { it[key(userId)] = gson.toJson(draft) }
    }

    override suspend fun clear(userId: String?) {
        context.draftDataStore.edit { it.remove(key(userId)) }
    }

    private fun key(userId: String?) = stringPreferencesKey("draft_json_${userId ?: "demo"}")
}
