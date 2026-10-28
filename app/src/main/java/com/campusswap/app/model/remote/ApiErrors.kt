package com.campusswap.app.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

object ApiErrors {
    fun message(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string() ?: return null
        val message = JsonParser.parseString(body).asJsonObject.get("message")
        if (message.isJsonArray) message.asJsonArray.joinToString { it.asString } else message.asString
    }.getOrNull()
}
