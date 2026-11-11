package com.campusswap.app.model.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Remembers whether the user chose the dark or the light theme. Nothing saved (null) means the app
 * follows the phone's setting. It belongs to the phone, not to an account, so log-out keeps it.
 */
interface ThemeStore {
    suspend fun read(): Boolean?
    suspend fun save(dark: Boolean)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreThemeStore(private val context: Context) : ThemeStore {
    override suspend fun read(): Boolean? = context.settingsDataStore.data.first()[DARK_THEME]

    override suspend fun save(dark: Boolean) {
        context.settingsDataStore.edit { it[DARK_THEME] = dark }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }
}
