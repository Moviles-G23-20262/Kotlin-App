package com.campusswap.app

import android.content.Context
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.MeetingPointPopularityRepository
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.location.CounterpartLocationSource
import com.campusswap.app.data.location.FusedLocationDataSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.SimulatedCounterpartLocation
import com.campusswap.app.data.remote.AnalyticsApi
import com.campusswap.app.data.remote.ApiClient
import com.campusswap.app.data.remote.CampusSwapApi
import com.campusswap.app.data.remote.RetrofitExchangeRemoteDataSource
import com.campusswap.app.data.remote.RetrofitMeetingPointRemoteDataSource
import com.campusswap.app.data.remote.RetrofitPopularityRemoteDataSource
import com.campusswap.app.domain.RankingStrategySelector
import com.campusswap.app.domain.TimeOfDayStrategySelector
import java.time.Clock

class AppContainer(context: Context) {
    private val api = ApiClient.create<CampusSwapApi>(BuildConfig.BASE_URL)

    private val analyticsApi = ApiClient.create<AnalyticsApi>(BuildConfig.ANALYTICS_BASE_URL)

    val locationDataSource: LocationDataSource = FusedLocationDataSource(context)

    val exchangeRepository = ExchangeRepository(RetrofitExchangeRemoteDataSource(api))

    val meetingPointRepository = MeetingPointRepository(RetrofitMeetingPointRemoteDataSource(api))

    val popularityRepository = MeetingPointPopularityRepository(RetrofitPopularityRemoteDataSource(analyticsApi))

    val counterpartLocationSource: CounterpartLocationSource = SimulatedCounterpartLocation()

    val rankingStrategySelector: RankingStrategySelector = TimeOfDayStrategySelector()

    val clock: Clock = Clock.systemDefaultZone()
}
