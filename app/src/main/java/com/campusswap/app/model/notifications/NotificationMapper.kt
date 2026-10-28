package com.campusswap.app.data.notifications

import com.campusswap.app.data.AppNotification
import com.campusswap.app.data.NotificationKind

fun NotificationDto.toAppNotification(): AppNotification {
    val isMatch = type == "SMART_MATCH"
    val isOrder = type == "ORDER_PLACED"
    val productTitle = material?.title

    return AppNotification(
        id = id,
        title = when { isMatch -> "Alert match"; isOrder -> "New order"; else -> "Notification" },
        message = when {
            isMatch && productTitle != null -> "$productTitle matches one of your alerts"
            isOrder && productTitle != null -> "Someone ordered $productTitle"
            productTitle != null -> productTitle
            isMatch -> "A new listing matches one of your alerts"
            else -> "You have a new notification"
        },
        isRead = openedAt != null,
        kind = if (isMatch) NotificationKind.ALERT_MATCH else NotificationKind.GENERIC,
        productId = materialId,
    )
}