package com.campusswap.app.model.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

interface ChatDraftStore {
    suspend fun read(owner: String, chat: String): String?
    fun save(owner: String, chat: String, text: String)

    object None : ChatDraftStore {
        override suspend fun read(owner: String, chat: String): String? = null
        override fun save(owner: String, chat: String, text: String) = Unit
    }
}

private val Context.chatDrafts by preferencesDataStore(name = "chat_drafts")

class DataStoreChatDraftStore(
    private val context: Context,
    private val scope: CoroutineScope,
) : ChatDraftStore {
    private val writes = ConcurrentHashMap<String, Job>()

    override suspend fun read(owner: String, chat: String): String? =
        context.chatDrafts.data.first()[key(owner, chat)]

    override fun save(owner: String, chat: String, text: String) {
        val key = key(owner, chat)
        writes[key.name]?.cancel()
        writes[key.name] = scope.launch {
            delay(WRITE_DELAY_MS)
            context.chatDrafts.edit { if (text.isBlank()) it.remove(key) else it[key] = text }
        }
    }

    private fun key(owner: String, chat: String) = stringPreferencesKey("$owner|$chat")

    private companion object {
        const val WRITE_DELAY_MS = 400L
    }
}
