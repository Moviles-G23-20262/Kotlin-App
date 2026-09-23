package com.campusswap.app.data.location

import com.campusswap.app.data.SampleData
import com.campusswap.app.domain.GeoPoint

/** Where the other party of an exchange is on campus. */
interface CounterpartLocationSource {
    suspend fun locationOf(productId: String): GeoPoint
}

/**
 * SIMULATED: the backend doesn't share the other user's position yet, so every seller is assumed to be at
 * the same campus spot. Replace with the location sent in the chat once meeting proposals go through the API (#13).
 */
class SimulatedCounterpartLocation : CounterpartLocationSource {
    override suspend fun locationOf(productId: String): GeoPoint = SampleData.simulatedCounterpartLocation
}
