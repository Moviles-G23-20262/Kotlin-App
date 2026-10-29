package com.campusswap.app.viewmodel

import com.campusswap.app.model.AlertMatch
import com.campusswap.app.model.AppNotification
import com.campusswap.app.model.CartLine
import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.Course
import com.campusswap.app.model.FeedbackTag
import com.campusswap.app.model.MeetingProposal
import com.campusswap.app.model.NotificationKind
import com.campusswap.app.model.Order
import com.campusswap.app.model.PendingExchange
import com.campusswap.app.model.PendingSync
import com.campusswap.app.model.Product
import com.campusswap.app.model.ProposalStatus
import com.campusswap.app.model.SampleData
import com.campusswap.app.model.SellDraft
import com.campusswap.app.model.Seller
import com.campusswap.app.model.SmartAlert
import com.campusswap.app.model.TransactionRating
import com.campusswap.app.model.domain.InputLimits
import com.campusswap.app.model.domain.InputValidation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.analytics.Events
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.campusswap.app.model.alerts.SmartAlertRepository
import com.campusswap.app.model.notifications.NotificationRepository
import com.campusswap.app.model.materials.MaterialRepository
import com.campusswap.app.model.connectivity.ConnectivityObserver
import com.campusswap.app.model.local.PendingActionEntity
import com.campusswap.app.model.local.PendingListingEntity
import com.campusswap.app.model.local.PendingMessageEntity
import com.campusswap.app.model.sync.OutboxRepository
import com.campusswap.app.model.sync.PendingActionKind
import com.campusswap.app.model.sync.RatingPayload
import com.campusswap.app.model.sync.WISHLIST_KINDS
import com.campusswap.app.model.sync.payloadAs
import com.campusswap.app.model.ratings.RatingRepository
import com.campusswap.app.model.ratings.RatingResult
import com.campusswap.app.model.auth.Session
import com.campusswap.app.model.materials.maskEmail
import com.campusswap.app.model.wishlist.WishlistRepository
import com.campusswap.app.model.wishlist.WishlistResult
import com.campusswap.app.model.orders.OrderRepository
import com.campusswap.app.model.orders.PlaceOrderResult
import com.campusswap.app.model.ratings.RatingDto
import com.campusswap.app.model.orders.Sale
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import androidx.compose.runtime.snapshotFlow
import com.campusswap.app.model.local.CartStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * Single in-memory state holder for the whole prototype. There is no backend:
 * every action here just mutates local Compose state so the UI behaves consistently
 * across screens (cart badge, wishlist hearts, newly published listings, etc.).
 */
class AppViewModel : ViewModel() {
    var isDarkTheme by mutableStateOf(true)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    var currentUserId by mutableStateOf<String?>(null)
        private set    

    var currentUser by mutableStateOf(SampleData.currentSeller)
        private set

    val allProducts = mutableStateListOf<Product>()

    // ---- Eventual connectivity ----

    var isOnline by mutableStateOf(true)
        private set

    /** False when Home is showing the on-device copy of the listings because the server couldn't be reached. */
    var materialsLive by mutableStateOf(true)
        private set

    var materialsUpdatedAtMillis by mutableStateOf<Long?>(null)
        private set

    private var outbox: OutboxRepository? = null
    private var materialRepository: MaterialRepository? = null
    private var sessionUserId: () -> String? = { null }

    /**
     * Starts the offline machinery once per app run: follows the network state, refreshes the
     * listings and sends the outbox when the connection comes back, and mirrors the outbox
     * (Room) into the UI so queued listings show their state.
     */
    fun startOfflineSync(
        connectivity: ConnectivityObserver,
        outbox: OutboxRepository,
        materials: MaterialRepository,
        sessionUserId: () -> String?,
    ) {
        if (this.outbox != null) return
        this.outbox = outbox
        this.materialRepository = materials
        this.sessionUserId = sessionUserId
        viewModelScope.launch {
            var wasOnline = connectivity.isOnline.value
            connectivity.isOnline.collect { online ->
                isOnline = online
                if (online && !wasOnline) onBackOnline()
                wasOnline = online
            }
        }
        viewModelScope.launch { outbox.pendingListings.collect { allPendingListings = it; showPendingListings() } }
        viewModelScope.launch { outbox.pendingActions.collect { allPendingActions = it; showPendingRatings(); showPendingWishlist() } }
        if (sessionUserId() != null) outbox.syncNow()
    }

    private var allPendingListings: List<PendingListingEntity> = emptyList()

    /** The outbox may hold another account's items; each user only sees their own. */
    private fun refreshPendingViews() {
        showPendingListings()
        showPendingRatings()
        showPendingWishlist()
    }

    private fun onBackOnline() {
        if (sessionUserId() != null) {
            materialRepository?.let(::loadMaterials)
            outbox?.syncNow()
            wishlistRepository?.let(::loadWishlist)
            orderRepository?.let(::loadOrders)
            smartAlertRepository?.let(::loadAlerts)
            notificationRepository?.let { loadNotifications(it, markRead = false) }
        }
    }

    private var pendingListingIds: Set<Long> = emptySet()

    private fun showPendingListings() {
        val rows = allPendingListings.filter { it.ownerId == currentUserId }
        val published = pendingListingIds - rows.map { it.localId }.toSet()
        pendingListingIds = rows.map { it.localId }.toSet()
        allProducts.removeAll { it.pendingSync != null }
        allProducts.addAll(0, rows.reversed().map { it.toProduct() })
        // A listing left the outbox because the server accepted it: fetch it back as a real listing.
        if (published.isNotEmpty()) materialRepository?.let(::loadMaterials)
    }

    private fun PendingListingEntity.toProduct() = Product(
        id = pendingProductId(localId),
        title = title,
        description = description,
        price = price.toDoubleOrNull() ?: 0.0,
        category = Category.entries.firstOrNull { it.name == category } ?: Category.SUPPLIES,
        course = courseCode?.let { code -> SampleData.courses.firstOrNull { it.code == code } ?: Course(code, code) },
        condition = Condition.entries.firstOrNull { it.name == condition } ?: Condition.GOOD,
        rating = null,
        reviewCount = 0,
        seller = currentUser,
        imageSeed = (localId % 12).toInt(),
        pendingSync = PendingSync(localId, failedReason),
        imageUrls = photoPaths?.split("\n")?.filter { it.isNotBlank() }?.map { "file://$it" }.orEmpty(),
    )

    fun discardPendingListing(product: Product) {
        val sync = product.pendingSync ?: return
        viewModelScope.launch { outbox?.discardListing(sync.localId) }
    }

    val cart = mutableStateListOf<CartLine>()


    private var cartSaver: Job? = null

    @OptIn(FlowPreview::class)
    fun restoreCart(store: CartStore) {
        val userId = currentUserId ?: return
        cartSaver?.cancel()
        cartSaver = viewModelScope.launch {
            val saved = store.read(userId)
            if (cart.isEmpty()) cart.addAll(saved)
            snapshotFlow { cart.toList() }
                .drop(1) // la primera emisión es lo que se acaba de leer
                .debounce(CART_SAVE_DEBOUNCE_MS)
                .collectLatest { lines -> store.save(userId, lines) }
        }
    }

    val wishlist = mutableStateListOf<String>()

    /** Productos guardados en el backend incluye vendidos/reservados, que no salen en el feed */
    val wishlistProducts = mutableStateListOf<Product>()

    /** Listings only reachable through a conversation: they left the feed (reserved, sold) but the chat is still open. */
    val chatProducts = mutableStateListOf<Product>()

    fun rememberChatProduct(product: Product) {
        if (chatProducts.none { it.id == product.id }) chatProducts.add(product)
    }

    private var wishlistRepository: WishlistRepository? = null

    fun loadWishlist(repository: WishlistRepository) {
        wishlistRepository = repository
        viewModelScope.launch {
            // null = sin red: se quedan los corazones que ya había en pantalla
            val saved = repository.load() ?: return@launch
            wishlist.clear()
            wishlist.addAll(saved.map { it.id })
            wishlistProducts.clear()
            wishlistProducts.addAll(saved)
            // Lo que se marcó sin red todavía no está en el servidor: se vuelve a aplicar encima
            showPendingWishlist()
        }
    }

    /** Aviso corto sobre un favorito (guardado sin red, rechazado...); la app lo muestra como snackbar. */
    var wishlistMessage by mutableStateOf<String?>(null)
        private set

    fun clearWishlistMessage() {
        wishlistMessage = null
    }

    /**
     * Mirrors the wishlist changes waiting in the outbox: hearts tapped offline stay as the user
     * left them (also after a restart), and a change the server refused is undone with its reason.
     */
    private fun showPendingWishlist() {
        val mine = allPendingActions.filter { it.ownerId == currentUserId && it.kind in WISHLIST_KINDS }
        mine.forEach { row ->
            val added = row.kind == PendingActionKind.WISHLIST_ADD.name
            if (row.failedReason == null) {
                showWishlistChange(row.productId, added)
            } else {
                showWishlistChange(row.productId, !added)
                wishlistMessage = row.failedReason
                viewModelScope.launch { outbox?.discardAction(row.localId) }
            }
        }
    }

    private fun showWishlistChange(productId: String, added: Boolean) {
        if (added) {
            if (productId !in wishlist) wishlist.add(productId)
            if (wishlistProducts.none { it.id == productId }) findProduct(productId)?.let { wishlistProducts.add(0, it) }
        } else {
            wishlist.remove(productId)
            wishlistProducts.removeAll { it.id == productId }
        }
    }

    val notifications = mutableStateListOf<AppNotification>()

    /** Set by Home's "See all" before navigating to Search, then consumed once. */
    var pendingSearchCategory: Category? = null

    val myListings get() = allProducts.filter { it.seller.id == currentUser.id }

    val cartTotal get() = cart.sumOf { it.product.price * it.quantity }

    val unreadNotificationCount get() = notifications.count { !it.isRead }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    fun login(userId: String? = null) {
        currentUserId = userId
        isLoggedIn = true
        refreshPendingViews()
        Analytics.log(Events.LOGIN_SUCCESS)
    }

    fun checkCredentials(username: String, password: String): Boolean =
        username.trim() == DEMO_USERNAME && password == DEMO_PASSWORD

    val featuredSellers: List<FeaturedSeller>
        get() = allProducts
            .groupBy { it.seller.id }
            .filterKeys { it != currentUser.id }
            .map { (_, items) -> FeaturedSeller(items.first().seller, items.size) }
            .sortedByDescending { it.seller.rating ?: 0.0 }

    val serviceFee: Double get() = if (cart.isEmpty()) 0.0 else SERVICE_FEE

    val orderTotal: Double get() = cartTotal + serviceFee

    val orders = mutableStateListOf<Order>()
    private var orderRepository: OrderRepository? = null
    /** false cuando compras y ventas son la copia guardada en el tel */
    var ordersLive by mutableStateOf(true)
        private set

    var ordersSavedAtMillis by mutableStateOf<Long?>(null)
        private set


    /** Tus ventas del backend lo que compraron (reservado o vendido) */
    val sales = mutableStateListOf<Sale>()

    /** Lo que ganaste en ventas completadas este mes */
    val earnedThisMonth: Double
        get() {
            val thisMonth = YearMonth.now()
            return sales
                .filter { it.status == "COMPLETED" }
                .filter { sale ->
                    sale.completedAtMillis?.let { YearMonth.from(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) } == thisMonth
                }
                .sumOf { it.product.price }
        }

    /** Signed-in orders are placed on the server, so checkout waits for a connection. */
    val needsConnectionToOrder: Boolean get() = orderRepository != null && !isOnline

    /** Por qué no se pudo hacer el pedido (null = sin problema); lo muestra el checkout. */
    var checkoutMessage by mutableStateOf<String?>(null)
        private set

    /** Compras del usuario guardadas en el backend: sobreviven a cerrar la app. */
    fun loadOrders(repository: OrderRepository) {
        orderRepository = repository
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val loaded = repository.load(userId) ?: return@launch
            orders.clear()
            orders.addAll(loaded.purchases)
            sales.clear()
            sales.addAll(loaded.sales)
            ordersLive = loaded.isLive
            ordersSavedAtMillis = loaded.savedAtMillis
        }
    }

    fun findProduct(id: String): Product? =
        allProducts.find { it.id == id }
            ?: orders.firstNotNullOfOrNull { order -> order.products.find { it.id == id } }
            ?: wishlistProducts.find { it.id == id }
            ?: sales.firstNotNullOfOrNull { sale -> sale.product.takeIf { it.id == id } }
            ?: chatProducts.find { it.id == id }

    /** Pide al backend los productos del carrito; devuelve # de orden */
    suspend fun placeOrder(meetOnCampus: Boolean = true): Int? {
        checkoutMessage = null
        val repository = orderRepository
        val order = if (repository == null) {
            // Acceso demo (sin sesión): la orden solo existe en el teléfono, como antes
            Order(number = (1000..9999).random(), lines = cart.toList(), meetOnCampus = meetOnCampus)
        } else {
            when (val result = repository.place(cart.toList(), meetOnCampus)) {
                PlaceOrderResult.Offline -> {
                    checkoutMessage = "You're offline. Your cart is saved: place the order when you're back online."
                    return null
                }
                is PlaceOrderResult.Rejected -> {
                    cart.removeAll { line -> result.unavailable.any { it.id == line.product.id } }
                    checkoutMessage = "No longer available: ${result.unavailable.joinToString { it.title }}"
                    return null
                }
                is PlaceOrderResult.Placed -> result.order
            }
        }
        orders.add(0, order)
        cart.clear()
        order.products.forEach { product ->
            Analytics.log(
                Events.PURCHASE_COMPLETED,
                "product_id" to product.id,
                "category" to product.category.name,
                "price" to product.price,
                "after_match_notification" to notifications.any { it.kind == NotificationKind.ALERT_MATCH && it.productId == product.id },
                "was_wishlisted" to isWishlisted(product.id),
            )
            notify(
                id = "order-${order.number}-${product.id}",
                title = "Order #CSW-${order.number} confirmed",
                message = "Agree on a meeting point with ${product.seller.name.substringBefore(' ')}, then close the exchange and rate them.",
                kind = NotificationKind.EXCHANGE,
                productId = product.id,
            )
        }
        if (repository != null) loadOrders(repository)
        return order.number
    }

    val pendingExchanges: List<PendingExchange>
        get() = orders.flatMap { order ->
            order.products.map { product ->
                PendingExchange(
                    order = order,
                    product = product,
                    proposal = meetingProposals[product.id],
                    rating = ratings[product.id],
                )
            }
        }

    fun orderFor(productId: String): Order? =
        orders.firstOrNull { order -> order.products.any { it.id == productId } }

    fun hasPendingExchange(productId: String): Boolean =
        orderFor(productId) != null && ratings[productId] == null

    fun restoreSession(userId: String) {
        currentUserId = userId
        isLoggedIn = true
        refreshPendingViews()
    }

    // Usa los datos reales no el usuario de ejemplo
    fun useSession(session: Session) {
        currentUserId = session.userId
        currentUser = Seller(
            id = session.userId,
            name = session.fullName,
            isVerified = true,
            rating = null,
            reviewCount = 0,
            maskedEmail = maskEmail(session.email),
        )
    }    

    fun logout() {
        isLoggedIn = false
        cartSaver?.cancel()
        cartSaver = null
        cart.clear()
        currentUserId = null
        currentUser = SampleData.currentSeller
        notifications.clear()
        smartAlertRepository = null
        alerts.clear()
        alerts.addAll(SampleData.alerts)
        alertsLive = true
        refreshPendingViews()
        wishlist.clear()
        wishlistProducts.clear()
        wishlistRepository?.clear()
        orders.clear()
        ratings.clear()
        ratingSync.clear()
        receivedRatings.clear()
        myReviews.clear()
        sales.clear()
        ordersLive = true
        ordersSavedAtMillis = null
        // The next log-in may be demo access: nothing may keep talking to the server with the old account.
        wishlistRepository = null
        orderRepository = null
        notificationRepository = null
        loadedNotificationIds = emptySet()
        notificationsLive = true
        checkoutMessage = null
        alertError = null
        wishlistMessage = null
        chatProducts.clear()
        meetingProposals.clear()
        reservedMatches.clear()
        shownMatches.clear()
        pendingSearchCategory = null
    }

    fun isWishlisted(productId: String) = wishlist.contains(productId)

    fun toggleWishlist(productId: String) {
        val added = !wishlist.remove(productId)
        if (added) wishlist.add(productId)
        viewModelScope.launch {
            val queue = outbox
            val userId = sessionUserId()
            // Deshace un cambio que aún no salió del teléfono: no hay nada que pedirle al servidor
            if (queue != null && userId != null && queue.undoPendingWishlist(userId, productId, added)) {
                showWishlistChange(productId, added)
                return@launch
            }
            // Sin repositorio (acceso demo) se queda solo en el teléfono, como antes
            val repository = wishlistRepository
            val result = when {
                repository == null -> WishlistResult.SAVED
                added -> repository.add(productId)
                else -> repository.remove(productId)
            }
            when {
                result == WishlistResult.SAVED -> {
                    Analytics.log(Events.WISHLIST_TOGGLED, "product_id" to productId, "added" to added)
                    showWishlistChange(productId, added)
                }
                // Sin red: el corazón se queda como lo dejó el usuario y el Outbox lo envía al volver la señal
                result == WishlistResult.OFFLINE && queue != null && userId != null -> {
                    queue.queueWishlist(userId, productId, added)
                    Analytics.log(Events.WISHLIST_TOGGLED, "product_id" to productId, "added" to added, "queued" to true)
                    showWishlistChange(productId, added)
                    wishlistMessage = "You're offline. Saved on this phone: it will sync when you're back online."
                }
                else -> {
                    if (added) wishlist.remove(productId) else wishlist.add(productId)
                    wishlistMessage = when (result) {
                        WishlistResult.NOT_ON_SERVER -> "You can save this listing once it's published."
                        WishlistResult.OFFLINE -> "You're offline. Try again when you're back online."
                        else -> "Couldn't update your wishlist. Try again."
                    }
                }
            }
        }
    }

    fun addToCart(product: Product) {
        Analytics.log(Events.CART_ADD, "product_id" to product.id, "category" to product.category.name)
        val index = cart.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = cart[index]
            cart[index] = existing.copy(quantity = (existing.quantity + 1).coerceAtMost(InputLimits.CART_QUANTITY_MAX))
        } else {
            cart.add(CartLine(product, 1))
        }
    }

    fun removeFromCart(productId: String) {
        cart.removeAll { it.product.id == productId }
    }

    fun setQuantity(productId: String, quantity: Int) {
        val index = cart.indexOfFirst { it.product.id == productId }
        if (index < 0) return
        if (quantity <= 0) {
            cart.removeAt(index)
        } else {
            cart[index] = cart[index].copy(quantity = quantity.coerceAtMost(InputLimits.CART_QUANTITY_MAX))
        }
    }

    // save searches & smart matches (View 11)

    val alerts = mutableStateListOf<SmartAlert>().apply { addAll(SampleData.alerts) }

    private val reservedMatches = mutableStateListOf<String>()

    val alertMatches: List<AlertMatch>
        get() = alerts.filter { it.enabled }.flatMap { alert ->
            allProducts
                .filter { it.seller.id != currentUser.id }
                .filter { product ->
                    product.price <= alert.maxPrice &&
                        product.condition.ordinal <= alert.minCondition.ordinal &&
                        (alert.course == null || product.course?.code == alert.course.code) &&
                        (alert.category == Category.ALL || product.category == alert.category) &&
                        (alert.keyword.isBlank() || product.title.contains(alert.keyword, ignoreCase = true) ||
                            product.course?.code?.contains(alert.keyword, ignoreCase = true) == true)
                }
                .map { product ->
                    AlertMatch(
                        alert = alert,
                        product = product,
                        postedMinutesAgo = product.createdAtMillis?.let { ((System.currentTimeMillis() - it) / 60_000).toInt() } ?: 0,                        reserved = reservedMatches.contains(product.id),
                    )
                }
        }.distinctBy { it.product.id }.sortedBy { it.postedMinutesAgo }

    val unseenMatchCount: Int
        get() = notifications.count { it.kind == NotificationKind.ALERT_MATCH && !it.isRead }


    private var smartAlertRepository: SmartAlertRepository? = null

    /** False while the alerts shown are the copy saved on the phone. */
    var alertsLive by mutableStateOf(true)
        private set

    var alertError by mutableStateOf<String?>(null)
        private set

    fun dismissAlertError() {
        alertError = null
    }

    /**
     * Signed-in students keep their alerts on the server, which notifies them when a matching
     * listing is published. Demo access keeps the sample alerts and matches them on the phone.
     */
    fun loadAlerts(repository: SmartAlertRepository) {
        smartAlertRepository = repository
        // From now on the server sends the match notifications; drop the ones made on the phone.
        notifications.removeAll { it.id.startsWith(LOCAL_MATCH_PREFIX) }
        viewModelScope.launch {
            val feed = repository.load() ?: return@launch
            alerts.clear()
            alerts.addAll(feed.alerts)
            alertsLive = feed.isLive
        }
    }

    fun publishMatchNotifications() {
        if (smartAlertRepository != null) return
        alertMatches.forEach { match ->
            val isNew = notify(
                id = "$LOCAL_MATCH_PREFIX${match.product.id}",
                title = "Alert match · ${match.alert.keyword.ifBlank { match.alert.course?.code ?: match.alert.category.label }}",
                message = "${match.product.title} — ${match.savingPercent}% below your limit.",
                kind = NotificationKind.ALERT_MATCH,
                productId = match.product.id,
            )
            if (isNew) Analytics.log(Events.MATCH_NOTIFIED, "product_id" to match.product.id, "alert_id" to match.alert.id)
        }
    }

    /** Shown right away; if the server can't save it, it's taken back and [alertError] says why. */
    fun addAlert(alert: SmartAlert) {
        alerts.add(0, alert)
        val repository = smartAlertRepository
        if (repository == null) {
            Analytics.log(Events.ALERT_CREATED, "alert_id" to alert.id)
            publishMatchNotifications()
            return
        }
        viewModelScope.launch {
            val saved = repository.create(alert)
            val index = alerts.indexOfFirst { it.id == alert.id }
            if (saved != null) {
                if (index >= 0) alerts[index] = saved
                Analytics.log(Events.ALERT_CREATED, "alert_id" to saved.id)
            } else {
                if (index >= 0) alerts.removeAt(index)
                alertError = ALERT_NOT_SAVED
            }
        }
    }

    fun toggleAlert(id: String) {
        val index = alerts.indexOfFirst { it.id == id }
        if (index < 0) return
        val toggled = alerts[index].copy(enabled = !alerts[index].enabled)
        alerts[index] = toggled
        val repository = smartAlertRepository ?: return
        viewModelScope.launch {
            if (repository.setEnabled(id, toggled.enabled)) return@launch
            alerts.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { alerts[it] = toggled.copy(enabled = !toggled.enabled) }
            alertError = ALERT_NOT_SAVED
        }
    }

    fun removeAlert(id: String) {
        val index = alerts.indexOfFirst { it.id == id }
        if (index < 0) return
        val removed = alerts.removeAt(index)
        val repository = smartAlertRepository ?: return
        viewModelScope.launch {
            if (repository.remove(id)) return@launch
            alerts.add(index.coerceAtMost(alerts.size), removed)
            alertError = ALERT_NOT_SAVED
        }
    }

    fun isReserved(productId: String) = reservedMatches.contains(productId)

    fun reserveMatch(productId: String) {
        if (reservedMatches.contains(productId)) return
        reservedMatches.add(productId)
        Analytics.log(Events.SMART_MATCH_RESERVED, "product_id" to productId)
    }

    private val shownMatches = mutableSetOf<String>()

    fun onMatchShown(productId: String, alertId: String) {
        if (shownMatches.add(productId)) Analytics.log(Events.SMART_MATCH_SHOWN, "product_id" to productId, "alert_id" to alertId)
    }

    fun openMatch(productId: String) {
        Analytics.log(Events.SMART_MATCH_OPENED, "product_id" to productId)
    }

    // transaction completion & rating (View 12)

    val ratings = mutableStateMapOf<String, TransactionRating>()

    fun ratingFor(productId: String, otherId: String? = null): TransactionRating? =
        ratings[productId]?.takeIf { otherId == null || it.ratedId == null || it.ratedId == otherId }

    fun submitRating(rating: TransactionRating) {
        ratings[rating.productId] = rating
        Analytics.log(Events.EXCHANGE_RATED, "product_id" to rating.productId)
    }

    val ratingSync = mutableStateMapOf<String, RatingResult>()

    val receivedRatings = mutableStateMapOf<String, RatingDto>()

    /** Calificaciones que de usuario  */
    val myReviews = mutableStateListOf<RatingDto>()

    /** Promedio real de estrellas recibidas */
    val myRating: Double?
        get() = myReviews.takeIf { it.isNotEmpty() }?.map { it.stars }?.average()

    fun loadMyReviews(repository: RatingRepository) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val reviews = repository.received(userId) ?: return@launch
            myReviews.clear()
            myReviews.addAll(reviews)
        }
    }

    /**ya califique este intercambio? la otra persona ya me califico? */
    fun loadExchangeRatings(repository: RatingRepository, product: Product, otherId: String? = null) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val found = repository.exchangeRatings(product.id, userId, otherId) ?: return@launch
            found.mine?.let { mine ->
                // Ya estaba guardada en el backend: se muestra como enviada, sin volver a pedirla
                if (ratingFor(product.id, otherId) == null) ratings[product.id] = mine.toTransactionRating(product)
                ratingSync[product.id] = RatingResult.Saved
            }
            found.theirs?.let { receivedRatings[product.id] = it }
        }
    }

    private fun RatingDto.toTransactionRating(product: Product) = TransactionRating(
        productId = product.id,
        stars = stars,
        confirmedCondition = product.condition,
        // El backend guarda los tags como texto ("Punctual"); se vuelven a convertir en FeedbackTag
        tags = tags.orEmpty().mapNotNull { label -> FeedbackTag.entries.firstOrNull { it.label == label } }.toSet(),
        review = review.orEmpty(),
        ratedId = ratedId,
    )

    fun sendRating(repository: RatingRepository, ratedUserId: String, rating: TransactionRating) {
        val userId = currentUserId ?: return
        ratingSync.remove(rating.productId)
        viewModelScope.launch {
            // A rating the outbox couldn't deliver is replaced by this new attempt.
            pendingRatings[rating.productId]?.let { outbox?.discardAction(it.localId) }
            val result = repository.submit(
                productId = rating.productId,
                ratedId = ratedUserId,
                stars = rating.stars,
                tags = rating.tags.map { it.label },
                review = rating.review.ifBlank { null },
            )
            val queue = outbox
            ratingSync[rating.productId] = if (result == RatingResult.Offline && queue != null) {
                queue.queueRating(userId, ratedUserId, rating)
                RatingResult.Queued
            } else {
                result
            }
        }
    }

    private var allPendingActions: List<PendingActionEntity> = emptyList()
    private var pendingRatings: Map<String, PendingActionEntity> = emptyMap()

    /**
     * Mirrors the ratings waiting in the outbox: still queued, refused by the server, or gone
     * (delivered). After a restart it also brings back the rating the user wrote.
     */
    private fun showPendingRatings() {
        val mine = allPendingActions
            .filter { it.kind == PendingActionKind.RATING.name && it.ownerId == currentUserId }
            .associateBy { it.productId }
        (pendingRatings.keys - mine.keys).forEach { productId ->
            if (ratingSync[productId] == RatingResult.Queued) ratingSync[productId] = RatingResult.Saved
        }
        mine.forEach { (productId, row) ->
            ratingSync[productId] = row.failedReason?.let { RatingResult.Refused(it) } ?: RatingResult.Queued
            if (ratings[productId] == null) row.payloadAs<RatingPayload>()?.let { ratings[productId] = it.toTransactionRating(productId) }
        }
        pendingRatings = mine
    }

    private fun RatingPayload.toTransactionRating(productId: String) = TransactionRating(
        productId = productId,
        stars = stars,
        confirmedCondition = Condition.entries.firstOrNull { it.name == condition } ?: Condition.GOOD,
        tags = tags.mapNotNull { label -> FeedbackTag.entries.firstOrNull { it.label == label } }.toSet(),
        review = review.orEmpty(),
        ratedId = ratedId,
    )
    
    fun canCompleteExchange(productId: String) =
        meetingProposals[productId]?.status == ProposalStatus.ACCEPTED

    private fun notify(
        id: String,
        title: String,
        message: String,
        kind: NotificationKind,
        productId: String? = null,
    ): Boolean {
        if (notifications.any { it.id == id }) return false
        notifications.add(
            0,
            AppNotification(id, title, message, isRead = false, kind = kind, productId = productId),
        )
        return true
    }

    // No guarda si es demo y recuerda cuales vinieron del backend
    private var notificationRepository: NotificationRepository? = null
    private var loadedNotificationIds: Set<String> = emptySet()

    var notificationsLive by mutableStateOf(true)
        private set

    /** [markRead] is for the notifications screen; a background refresh keeps the unread badge. */
    fun loadNotifications(repository: NotificationRepository, markRead: Boolean = true) {
        notificationRepository = repository
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val feed = repository.load(userId)
            notificationsLive = feed.isLive
            notifications.removeAll { it.id in loadedNotificationIds }
            notifications.addAll(0, feed.items)
            loadedNotificationIds = feed.items.map { it.id }.toSet()
            if (markRead) markNotificationsRead()
        }
    }

    fun markNotificationsRead() {
        val unreadFromBackend = notifications.filter { !it.isRead && it.id in loadedNotificationIds }
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
        val repository = notificationRepository ?: return
        if (!notificationsLive) return
        viewModelScope.launch {
            unreadFromBackend.forEach { repository.markOpened(it.id) }
        }
    }

    private var loadedMaterialIds: Set<String> = emptySet()

    fun loadMaterials(repository: MaterialRepository) {
        viewModelScope.launch {
            val feed = repository.load()
            materialsLive = feed.isLive
            materialsUpdatedAtMillis = feed.updatedAtMillis
            allProducts.removeAll { it.id in loadedMaterialIds }
            allProducts.addAll(allProducts.count { it.pendingSync != null }, feed.products)
            loadedMaterialIds = feed.products.map { it.id }.toSet()
        }
    }

    fun isRemoteNotification(id: String) = id in loadedNotificationIds

    // ---- Meeting coordination cache (Views 09 / 10) ----

    /**
     * Latest proposal per product, published by the chat and meeting screens so the home
     * card and the completion screen know whether the exchange can be closed.
     */
    val meetingProposals = mutableStateMapOf<String, MeetingProposal>()

    fun cacheProposal(productId: String, proposal: MeetingProposal) {
        meetingProposals[productId] = proposal
    }

    /**
     * Signed-in users publish through the outbox, so the listing survives having no signal and is
     * sent by WorkManager when possible. Demo access keeps the listing on the phone only.
     */
    suspend fun publishListing(draft: SellDraft): Product {
        val queue = outbox
        val userId = sessionUserId()
        if (queue != null && userId != null) {
            val product = queue.queueListing(draft, userId).toProduct()
            logListingPublished(draft, product.price)
            // Room's Flow adds it to the feed too; adding it now means the listing can be opened right away.
            if (allProducts.none { it.id == product.id }) allProducts.add(0, product)
            return product
        }
        val product = Product(
            id = "local-${allProducts.size + 1}",
            title = draft.title.trim(),
            description = draft.description.trim(),
            price = draft.price.toDoubleOrNull() ?: 0.0,
            category = draft.category,
            course = if (draft.notAssociatedWithCourse) null else draft.course,
            condition = draft.condition ?: Condition.GOOD,
            rating = null,
            reviewCount = 0,
            seller = currentUser,
            imageSeed = (allProducts.size + 1),
            imageUrls = draft.photoPaths.map { "file://$it" },
        )
        allProducts.add(0, product)
        logListingPublished(draft, product.price)
        return product
    }

    private fun logListingPublished(draft: SellDraft, price: Double) {
        Analytics.log(
            Events.LISTING_PUBLISHED,
            "category" to draft.category.name,
            "price" to price,
            "photo_count" to draft.photoCount,
            "course_empty" to (draft.course == null),
            "condition_empty" to (draft.condition == null),
            "description_length" to draft.description.length,
        )
        publishMatchNotifications()
    }

    init {
        publishMatchNotifications()
    }

    private companion object {
        const val LOCAL_MATCH_PREFIX = "match-"
        const val ALERT_NOT_SAVED = "We couldn't save that change to your alerts. Check your connection and try again."
        const val CART_SAVE_DEBOUNCE_MS = 300L    
    }
}

data class FeaturedSeller(
    val seller: Seller,
    val itemCount: Int,
)

fun pendingProductId(localId: Long) = "pending-$localId"

const val DEMO_USERNAME = "uwu"
const val DEMO_PASSWORD = "uwu123"
const val SERVICE_FEE = 2000.0
const val HOME_DELIVERY_FEE = 10000.0
