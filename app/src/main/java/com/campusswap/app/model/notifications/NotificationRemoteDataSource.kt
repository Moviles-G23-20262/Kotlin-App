package com.campusswap.app.data.notifications

interface NotificationRemoteDataSource {
    // suspend sirve para no congelar la app mientras se hace la llamada a la api
    suspend fun getNotifications(): List<NotificationDto>
    suspend fun markOpened(id: String, openedAt: String)
}