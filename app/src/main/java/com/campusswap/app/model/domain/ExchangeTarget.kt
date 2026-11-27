package com.campusswap.app.model.domain

data class ExchangeTarget(
    val productId: String,
    val buyerId: String,
    val sellerId: String,
    val price: Double,
    val meetingPointId: String?,
    val meetingPoint: GeoPoint?,
    /** The signed-in user is the seller handing the item over, not the buyer receiving it. */
    val asSeller: Boolean = false,
)
