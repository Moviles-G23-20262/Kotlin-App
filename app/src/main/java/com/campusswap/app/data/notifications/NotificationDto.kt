package com.campusswap.app.data.notifications

data class NotificationDto(
    val id: String,
    val userId: String,
    val materialId: String?,        // puede ser null
    val type: String,       // smart_match u otro 
    val sentAt: String,
    val openedAt: String?,      // puede ser null
    val material: MaterialSummaryDto? = null,
)

data class MaterialSummaryDto(
    val id: String,
    val title: String,
)