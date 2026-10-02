package com.campusswap.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface SessionStore {
    suspend fun read(): Session?
    suspend fun save(session: Session)
    suspend fun clear()
}

private val Context.sessionDataStore by preferencesDataStore(name = "session")

class DataStoreSessionStore(private val context: Context) : SessionStore {

    override suspend fun read(): Session? {
        val prefs = context.sessionDataStore.data.first()
        return Session(
            token = prefs[TOKEN] ?: return null,
            userId = prefs[USER_ID] ?: return null,
            email = prefs[EMAIL].orEmpty(),
            fullName = prefs[FULL_NAME].orEmpty(),
        )
    }

    override suspend fun save(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[TOKEN] = session.token
            prefs[USER_ID] = session.userId
            prefs[EMAIL] = session.email
            prefs[FULL_NAME] = session.fullName
        }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val FULL_NAME = stringPreferencesKey("full_name")
    }
}
