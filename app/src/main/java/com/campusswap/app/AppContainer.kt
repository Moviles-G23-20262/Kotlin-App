package com.campusswap.app

import android.content.Context
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.location.CounterpartLocationSource
import com.campusswap.app.data.location.FusedLocationDataSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.SimulatedCounterpartLocation
import com.campusswap.app.data.remote.ApiClient
import com.campusswap.app.data.remote.RetrofitExchangeRemoteDataSource
import com.campusswap.app.data.remote.RetrofitMeetingPointRemoteDataSource
import com.campusswap.app.domain.RankingStrategySelector
import com.campusswap.app.domain.TimeOfDayStrategySelector
import java.time.Clock

class AppContainer(context: Context) {
    private val api = ApiClient.create()

    val locationDataSource: LocationDataSource = FusedLocationDataSource(context)

    val exchangeRepository = ExchangeRepository(RetrofitExchangeRemoteDataSource(api))

    val meetingPointRepository = MeetingPointRepository(RetrofitMeetingPointRemoteDataSource(api))

    val counterpartLocationSource: CounterpartLocationSource = SimulatedCounterpartLocation()

    val rankingStrategySelector: RankingStrategySelector = TimeOfDayStrategySelector()

    // Injected so tests can pin the time of day; on a device it follows the system clock.
    val clock: Clock = Clock.systemDefaultZone()
}
