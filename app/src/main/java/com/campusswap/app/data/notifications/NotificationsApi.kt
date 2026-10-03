package com.campusswap.app.data.notifications

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationsApi {
    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @POST("notifications/{id}/open")
    suspend fun markOpened(@Path("id") id: String)
}

class RetrofitNotificationRemoteDataSource(
    private val api: NotificationsApi,
) : NotificationRemoteDataSource {
    override suspend fun getNotifications() = api.getNotifications()

    override suspend fun markOpened(id: String, openedAt: String) {
        api.markOpened(id)
    }
}