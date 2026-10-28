package com.campusswap.app.data.remote

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val backend: HttpUrl,
    private val token: () -> String?,
    private val onUnauthorized: () -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val current = token()
        if (current == null || !isBackend(request.url())) return chain.proceed(request)

        val response = chain.proceed(request.newBuilder().header("Authorization", "Bearer $current").build())
        if (response.code() == 401) onUnauthorized()
        return response
    }

    private fun isBackend(url: HttpUrl) =
        url.scheme() == backend.scheme() && url.host() == backend.host() && url.port() == backend.port()
}
