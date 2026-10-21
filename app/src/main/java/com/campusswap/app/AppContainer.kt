package com.campusswap.app

import android.content.Context
import com.campusswap.app.data.ChatRepository
import com.campusswap.app.data.ChatRoomRepository
import com.campusswap.app.data.ConversationsRepository
import com.campusswap.app.data.ExchangeRepository
import com.campusswap.app.data.MeetingPointPopularityRepository
import com.campusswap.app.data.MeetingPointRepository
import com.campusswap.app.data.MeetingProposalRepository
import com.campusswap.app.data.auth.AuthRepository
import com.campusswap.app.data.auth.DataStoreSessionStore
import com.campusswap.app.data.auth.SessionManager
import com.campusswap.app.data.location.CounterpartLocationSource
import com.campusswap.app.data.location.FusedLocationDataSource
import com.campusswap.app.data.location.LocationDataSource
import com.campusswap.app.data.location.ProposedMeetingLocation
import com.campusswap.app.analytics.HttpEventSink
import com.campusswap.app.data.remote.AnalyticsApi
import com.campusswap.app.data.remote.AnalyticsEventsApi
import com.campusswap.app.data.remote.AuthApi
import com.campusswap.app.data.remote.AuthInterceptor
import com.campusswap.app.data.remote.ApiClient
import com.campusswap.app.data.remote.CampusSwapApi
import com.campusswap.app.data.remote.ExchangesApi
import com.campusswap.app.data.remote.RetrofitAuthRemoteDataSource
import com.campusswap.app.data.remote.RetrofitExchangeRemoteDataSource
import com.campusswap.app.data.remote.RetrofitMeetingPointRemoteDataSource
import com.campusswap.app.data.remote.ChatRoomsApi
import com.campusswap.app.data.remote.MessagesApi
import com.campusswap.app.data.remote.MeetingProposalApi
import com.campusswap.app.data.remote.RetrofitChatRoomRemoteDataSource
import com.campusswap.app.data.remote.RetrofitMessageRemoteDataSource
import com.campusswap.app.data.remote.RetrofitUploadRemoteDataSource
import com.campusswap.app.data.remote.UploadsApi
import com.campusswap.app.data.remote.RetrofitMeetingProposalRemoteDataSource
import com.campusswap.app.data.remote.RetrofitPopularityRemoteDataSource
import com.campusswap.app.domain.RankingStrategySelector
import com.campusswap.app.domain.TimeOfDayStrategySelector
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import com.campusswap.app.data.notifications.NotificationRepository
import com.campusswap.app.data.notifications.NotificationsApi
import com.campusswap.app.data.notifications.RetrofitNotificationRemoteDataSource
import com.campusswap.app.data.materials.MaterialRepository
import com.campusswap.app.data.materials.MaterialsApi
import com.campusswap.app.data.materials.RetrofitMaterialRemoteDataSource
import com.campusswap.app.data.ratings.RatingRepository
import com.campusswap.app.data.ratings.RatingsApi
import com.campusswap.app.data.ratings.RetrofitRatingRemoteDataSource
import com.campusswap.app.data.connectivity.ConnectivityObserver
import com.campusswap.app.data.connectivity.NetworkConnectivityObserver
import com.campusswap.app.data.alerts.RetrofitSmartAlertRemoteDataSource
import com.campusswap.app.data.alerts.SmartAlertRepository
import com.campusswap.app.data.alerts.SmartAlertsApi
import com.campusswap.app.data.local.CampusSwapDatabase
import com.campusswap.app.data.local.DataStoreDraftStore
import com.campusswap.app.data.local.DraftStore
import com.campusswap.app.data.sync.OutboxRepository
import com.campusswap.app.data.sync.OutboxSync
import com.campusswap.app.data.sync.RepositoryActionSender
import com.campusswap.app.data.sync.OutboxWorker
import com.campusswap.app.data.local.ImageCache
import com.campusswap.app.data.local.ImageDiskCache
import com.campusswap.app.data.local.ListingPhotoStore
import com.campusswap.app.data.local.CartStore
import com.campusswap.app.data.local.DataStoreCartStore
import com.campusswap.app.data.local.RoomResponseCache
import com.campusswap.app.data.sync.RemoteListingPhotoUploader
import com.campusswap.app.data.wishlist.RetrofitWishlistRemoteDataSource
import com.campusswap.app.data.wishlist.WishlistApi
import com.campusswap.app.data.wishlist.WishlistRepository
import com.campusswap.app.data.orders.OrderRepository
import com.campusswap.app.data.orders.OrdersApi
import com.campusswap.app.data.orders.RetrofitOrdersRemoteDataSource

class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()

    val sessionManager = SessionManager(DataStoreSessionStore(context), clock, CoroutineScope(SupervisorJob() + Dispatchers.IO))

    private val backendClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(HttpUrl.get(BuildConfig.BASE_URL), { sessionManager.token }, sessionManager::expire))
        .build()

    private val api = ApiClient.create<CampusSwapApi>(BuildConfig.BASE_URL, backendClient)

    val authRepository = AuthRepository(RetrofitAuthRemoteDataSource(ApiClient.create<AuthApi>(BuildConfig.BASE_URL, backendClient)), sessionManager)

    private val analyticsApi = ApiClient.create<AnalyticsApi>(BuildConfig.ANALYTICS_BASE_URL)

    private val notificationsApi = ApiClient.create<NotificationsApi>(BuildConfig.BASE_URL, backendClient)

    private val materialsApi = ApiClient.create<MaterialsApi>(BuildConfig.BASE_URL, backendClient)

    private val ratingsApi = ApiClient.create<RatingsApi>(BuildConfig.BASE_URL, backendClient)

    val locationDataSource: LocationDataSource = FusedLocationDataSource(context)

    private val database = CampusSwapDatabase.create(context)

    private val responseCache = RoomResponseCache(database.responses(), clock)

    val exchangeRepository = ExchangeRepository(
        RetrofitExchangeRemoteDataSource(ApiClient.create<ExchangesApi>(BuildConfig.BASE_URL, backendClient)),
    )

    val eventSink = HttpEventSink(ApiClient.create<AnalyticsEventsApi>(BuildConfig.BASE_URL, backendClient)) { sessionManager.token != null }

    val meetingPointRepository = MeetingPointRepository(RetrofitMeetingPointRemoteDataSource(api), responseCache)

    private val chatRoomRemote = RetrofitChatRoomRemoteDataSource(
        ApiClient.create<ChatRoomsApi>(BuildConfig.BASE_URL, backendClient),
    )

    private val messageRemote = RetrofitMessageRemoteDataSource(
        ApiClient.create<MessagesApi>(BuildConfig.BASE_URL, backendClient),
    )

    private val signedInUserId: () -> String? = { sessionManager.validSession()?.userId }

    val chatRoomRepository = ChatRoomRepository(chatRoomRemote)

    val conversationsRepository = ConversationsRepository(chatRoomRemote, signedInUserId, cache = responseCache)

    private val uploadRemote = RetrofitUploadRemoteDataSource(
        ApiClient.create<UploadsApi>(BuildConfig.BASE_URL, backendClient),
    )

    val chatRepository = ChatRepository(chatRoomRepository, messageRemote, uploadRemote, signedInUserId, cache = responseCache)

    val meetingProposalRepository = MeetingProposalRepository(
        rooms = chatRoomRepository,
        remote = RetrofitMeetingProposalRemoteDataSource(ApiClient.create<MeetingProposalApi>(BuildConfig.BASE_URL, backendClient)),
        cache = responseCache,
        currentUserId = signedInUserId,
        clock = clock,
    )

    val popularityRepository = MeetingPointPopularityRepository(RetrofitPopularityRemoteDataSource(analyticsApi))

    val counterpartLocationSource: CounterpartLocationSource =
        ProposedMeetingLocation(meetingProposalRepository, signedInUserId)

    val rankingStrategySelector: RankingStrategySelector = TimeOfDayStrategySelector()

    val notificationRepository = NotificationRepository(RetrofitNotificationRemoteDataSource(notificationsApi), cache = responseCache)

    private val materialRemote = RetrofitMaterialRemoteDataSource(materialsApi)

    val materialRepository = MaterialRepository(materialRemote, database.cachedMaterials(), clock)

    val connectivity: ConnectivityObserver = NetworkConnectivityObserver(context)

    val draftStore: DraftStore = DataStoreDraftStore(context)

    val cartStore: CartStore = DataStoreCartStore(context)

    val listingPhotos = ListingPhotoStore(context)

    val ratingRepository = RatingRepository(RetrofitRatingRemoteDataSource(ratingsApi), responseCache)

    val imageCache = ImageCache(ImageDiskCache(File(context.cacheDir, "images"), maxBytes = 50L * 1024 * 1024))

    private val wishlistApi = ApiClient.create<WishlistApi>(BuildConfig.BASE_URL, backendClient)

    val wishlistRepository = WishlistRepository(RetrofitWishlistRemoteDataSource(wishlistApi), signedInUserId, responseCache)

    val outboxSync = OutboxSync(
        database.outbox(),
        materialRemote,
        chatRoomRepository,
        messageRemote,
        RemoteListingPhotoUploader(listingPhotos, uploadRemote),
        RepositoryActionSender(exchangeRepository, ratingRepository, meetingProposalRepository, wishlistRepository, clock),
    )

    val outboxRepository = OutboxRepository(database.outbox(), clock) { OutboxWorker.schedule(context) }

    private val ordersApi = ApiClient.create<OrdersApi>(BuildConfig.BASE_URL, backendClient)

    val orderRepository = OrderRepository(RetrofitOrdersRemoteDataSource(ordersApi), responseCache)

    val smartAlertRepository = SmartAlertRepository(
        RetrofitSmartAlertRemoteDataSource(ApiClient.create<SmartAlertsApi>(BuildConfig.BASE_URL, backendClient)),
        signedInUserId,
        responseCache,
    )
}
