package com.campusswap.app.domain

/** The exchange being confirmed, using the app's local ids. [meetingPoint] is null when no location is known for the agreed spot. */
data class ExchangeTarget(
    val productId: String,
    val buyerId: String,
    val sellerId: String,
    val price: Double,
    val meetingPointId: String?,
    val meetingPoint: GeoPoint?,
)
