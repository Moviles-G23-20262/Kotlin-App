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
import com.campusswap.app.data.notifications.NotificationRepository
import com.campusswap.app.data.notifications.NotificationsApi
import com.campusswap.app.data.notifications.RetrofitNotificationRemoteDataSource
import com.campusswap.app.data.users.RetrofitUserRemoteDataSource
import com.campusswap.app.data.users.UserRepository
import com.campusswap.app.data.users.UsersApi
import com.campusswap.app.data.materials.MaterialRepository
import com.campusswap.app.data.materials.MaterialsApi
import com.campusswap.app.data.materials.RetrofitMaterialRemoteDataSource

class AppContainer(context: Context) {
    private val api = ApiClient.create<CampusSwapApi>(BuildConfig.BASE_URL)

    private val analyticsApi = ApiClient.create<AnalyticsApi>(BuildConfig.ANALYTICS_BASE_URL)

    private val notificationsApi = ApiClient.create<NotificationsApi>(BuildConfig.BASE_URL)

    private val usersApi = ApiClient.create<UsersApi>(BuildConfig.BASE_URL)

    private val materialsApi = ApiClient.create<MaterialsApi>(BuildConfig.BASE_URL)

    val locationDataSource: LocationDataSource = FusedLocationDataSource(context)

    val exchangeRepository = ExchangeRepository(RetrofitExchangeRemoteDataSource(api))

    val meetingPointRepository = MeetingPointRepository(RetrofitMeetingPointRemoteDataSource(api))

    val popularityRepository = MeetingPointPopularityRepository(RetrofitPopularityRemoteDataSource(analyticsApi))

    val counterpartLocationSource: CounterpartLocationSource = SimulatedCounterpartLocation()

    val rankingStrategySelector: RankingStrategySelector = TimeOfDayStrategySelector()

    val clock: Clock = Clock.systemDefaultZone()

    val notificationRepository = NotificationRepository(RetrofitNotificationRemoteDataSource(notificationsApi))

    val userRepository = UserRepository(RetrofitUserRemoteDataSource(usersApi))

    val materialRepository = MaterialRepository(RetrofitMaterialRemoteDataSource(materialsApi))
}
