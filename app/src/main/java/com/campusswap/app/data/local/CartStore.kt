package com.campusswap.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.campusswap.app.data.CartLine
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first

/** El carrito de cada usuario sobrevive a cerrar la app o quedarse sin señal antes de pedir. */
interface CartStore {
    suspend fun read(userId: String): List<CartLine>
    suspend fun save(userId: String, lines: List<CartLine>)
}

private val Context.cartDataStore by preferencesDataStore(name = "cart")

class DataStoreCartStore(private val context: Context, private val gson: Gson = Gson()) : CartStore {

    override suspend fun read(userId: String): List<CartLine> {
        val json = context.cartDataStore.data.first()[keyFor(userId)] ?: return emptyList()
        // Un carrito guardado por otra versión de la app puede no leerse: mejor vacío que un crash
        return runCatching { gson.fromJson<List<CartLine>>(json, object : TypeToken<List<CartLine>>() {}.type) }
            .getOrNull().orEmpty()
    }

    override suspend fun save(userId: String, lines: List<CartLine>) {
        context.cartDataStore.edit { prefs ->
            if (lines.isEmpty()) prefs.remove(keyFor(userId)) else prefs[keyFor(userId)] = gson.toJson(lines)
        }
    }

    /** Una llave por usuario: otra cuenta en el mismo tel nunca ve tu carrito */
    private fun keyFor(userId: String) = stringPreferencesKey("cart_$userId")
}