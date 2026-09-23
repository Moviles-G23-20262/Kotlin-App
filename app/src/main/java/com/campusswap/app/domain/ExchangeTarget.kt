package com.campusswap.app.domain

data class ExchangeTarget(
    val productId: String,
    val buyerId: String,
    val sellerId: String,
    val price: Double,
    val meetingPointId: String?,
    val meetingPoint: GeoPoint?,
)
