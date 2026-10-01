package com.campusswap.app.data.notifications

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationsApi {
    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("notifications/{id}")
    suspend fun markOpened(@Path("id") id: String, @Body body: Map<String, String>)
}

class RetrofitNotificationRemoteDataSource(
    private val api: NotificationsApi,
) : NotificationRemoteDataSource {
    override suspend fun getNotifications() = api.getNotifications()

    override suspend fun markOpened(id: String, openedAt: String) {
        api.markOpened(id, mapOf("openedAt" to openedAt))
    }
}