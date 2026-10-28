package com.campusswap.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campusswap.app.data.SellDraft
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

/** Keeps the listing being written, so closing the app or losing signal mid-form doesn't lose it. */
interface DraftStore {
    suspend fun read(): SellDraft?
    suspend fun save(draft: SellDraft)
    suspend fun clear()
}

private val Context.draftDataStore by preferencesDataStore(name = "sell_draft")

class DataStoreDraftStore(private val context: Context, private val gson: Gson = Gson()) : DraftStore {

    override suspend fun read(): SellDraft? {
        val json = context.draftDataStore.data.first()[DRAFT] ?: return null
        // A draft saved by an older app version may not parse; starting fresh is better than crashing.
        return runCatching { gson.fromJson(json, SellDraft::class.java) }.getOrNull()
    }

    override suspend fun save(draft: SellDraft) {
        context.draftDataStore.edit { it[DRAFT] = gson.toJson(draft) }
    }

    override suspend fun clear() {
        context.draftDataStore.edit { it.clear() }
    }

    private companion object {
        val DRAFT = stringPreferencesKey("draft_json")
    }
}
