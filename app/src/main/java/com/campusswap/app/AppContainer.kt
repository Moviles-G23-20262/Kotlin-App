package com.campusswap.app

import android.content.Context
import com.campusswap.app.model.ChatRepository
import com.campusswap.app.model.ChatRoomRepository
import com.campusswap.app.model.ConversationsRepository
import com.campusswap.app.model.ExchangeRepository
import com.campusswap.app.model.MeetingPointPopularityRepository
import com.campusswap.app.model.MeetingPointRepository
import com.campusswap.app.model.MeetingProposalRepository
import com.campusswap.app.model.auth.AuthRepository
import com.campusswap.app.model.auth.DataStoreSessionStore
import com.campusswap.app.model.auth.SessionManager
import com.campusswap.app.model.location.CounterpartLocationSource
import com.campusswap.app.model.location.FusedLocationDataSource
import com.campusswap.app.model.location.LocationDataSource
import com.campusswap.app.model.location.ProposedMeetingLocation
import com.campusswap.app.model.analytics.HttpEventSink
import com.campusswap.app.model.remote.AnalyticsApi
import com.campusswap.app.model.remote.AnalyticsEventsApi
import com.campusswap.app.model.remote.AuthApi
import com.campusswap.app.model.remote.AuthInterceptor
import com.campusswap.app.model.remote.ApiClient
import com.campusswap.app.model.remote.CampusSwapApi
import com.campusswap.app.model.remote.ExchangesApi
import com.campusswap.app.model.remote.RetrofitAuthRemoteDataSource
import com.campusswap.app.model.remote.RetrofitExchangeRemoteDataSource
import com.campusswap.app.model.remote.RetrofitMeetingPointRemoteDataSource
import com.campusswap.app.model.remote.ChatRoomsApi
import com.campusswap.app.model.remote.MessagesApi
import com.campusswap.app.model.remote.MeetingProposalApi
import com.campusswap.app.model.remote.RetrofitChatRoomRemoteDataSource
import com.campusswap.app.model.remote.RetrofitMessageRemoteDataSource
import com.campusswap.app.model.remote.RetrofitUploadRemoteDataSource
import com.campusswap.app.model.remote.UploadsApi
import com.campusswap.app.model.remote.RetrofitMeetingProposalRemoteDataSource
import com.campusswap.app.model.remote.RetrofitPopularityRemoteDataSource
import com.campusswap.app.model.domain.RankingStrategySelector
import com.campusswap.app.model.domain.TimeOfDayStrategySelector
import java.io.File
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import com.campusswap.app.model.notifications.NotificationRepository
import com.campusswap.app.model.notifications.NotificationsApi
import com.campusswap.app.model.notifications.RetrofitNotificationRemoteDataSource
import com.campusswap.app.model.materials.MaterialRepository
import com.campusswap.app.model.materials.MaterialsApi
import com.campusswap.app.model.materials.RetrofitMaterialRemoteDataSource
import com.campusswap.app.model.ratings.RatingRepository
import com.campusswap.app.model.ratings.RatingsApi
import com.campusswap.app.model.ratings.RetrofitRatingRemoteDataSource
import com.campusswap.app.model.connectivity.ConnectivityObserver
import com.campusswap.app.model.connectivity.NetworkConnectivityObserver
import com.campusswap.app.model.alerts.RetrofitSmartAlertRemoteDataSource
import com.campusswap.app.model.alerts.SmartAlertRepository
import com.campusswap.app.model.alerts.SmartAlertsApi
import com.campusswap.app.model.local.CampusSwapDatabase
import com.campusswap.app.model.local.DataStoreDraftStore
import com.campusswap.app.model.local.DraftStore
import com.campusswap.app.model.sync.OutboxRepository
import com.campusswap.app.model.sync.OutboxSync
import com.campusswap.app.model.sync.RepositoryActionSender
import com.campusswap.app.model.sync.OutboxWorker
import com.campusswap.app.model.local.ImageCache
import com.campusswap.app.model.local.ImageDiskCache
import com.campusswap.app.model.local.ListingPhotoStore
import com.campusswap.app.model.local.CartStore
import com.campusswap.app.model.local.DataStoreCartStore
import com.campusswap.app.model.local.RoomResponseCache
import com.campusswap.app.model.sync.RemoteListingPhotoUploader
import com.campusswap.app.model.wishlist.RetrofitWishlistRemoteDataSource
import com.campusswap.app.model.wishlist.WishlistApi
import com.campusswap.app.model.wishlist.WishlistRepository
import com.campusswap.app.model.orders.OrderRepository
import com.campusswap.app.model.orders.OrdersApi
import com.campusswap.app.model.orders.RetrofitOrdersRemoteDataSource

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

    val listingPhotos = ListingPhotoStore(context)

    private val photoUploader = RemoteListingPhotoUploader(listingPhotos, uploadRemote)

    val chatRepository = ChatRepository(chatRoomRepository, messageRemote, signedInUserId, cache = responseCache)

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

    val ratingRepository = RatingRepository(RetrofitRatingRemoteDataSource(ratingsApi), responseCache)

    val imageCache = ImageCache(ImageDiskCache(File(context.cacheDir, "images"), maxBytes = 50L * 1024 * 1024))

    private val wishlistApi = ApiClient.create<WishlistApi>(BuildConfig.BASE_URL, backendClient)

    val wishlistRepository = WishlistRepository(RetrofitWishlistRemoteDataSource(wishlistApi), signedInUserId, responseCache)

    val outboxSync = OutboxSync(
        database.outbox(),
        materialRemote,
        chatRoomRepository,
        messageRemote,
        photoUploader,
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
