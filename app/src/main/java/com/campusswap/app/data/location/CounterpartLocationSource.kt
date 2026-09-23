package com.campusswap.app.data.location

import com.campusswap.app.data.SampleData
import com.campusswap.app.domain.GeoPoint

interface CounterpartLocationSource {
    suspend fun locationOf(productId: String): GeoPoint
}

class SimulatedCounterpartLocation : CounterpartLocationSource {
    override suspend fun locationOf(productId: String): GeoPoint = SampleData.simulatedCounterpartLocation
}
