package com.campusswap.app.data

import com.campusswap.app.domain.InputLimits
import com.campusswap.app.domain.InputValidation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusswap.app.analytics.Analytics
import com.campusswap.app.analytics.Events
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.campusswap.app.data.notifications.NotificationRepository
import com.campusswap.app.data.materials.MaterialRepository
import com.campusswap.app.data.ratings.RatingRepository
import com.campusswap.app.data.ratings.RatingResult

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

    val currentUser = SampleData.currentSeller

    val allProducts = mutableStateListOf<Product>().apply { addAll(SampleData.products) }

    val cart = mutableStateListOf<CartLine>()

    val wishlist = mutableStateListOf<String>()

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


    fun placeOrder(meetOnCampus: Boolean = true): Int {
        val number = (1000..9999).random()
        val order = Order(number = number, lines = cart.toList(), meetOnCampus = meetOnCampus)
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
        return number
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
    }

    fun logout() {
        isLoggedIn = false
        cart.clear()
        currentUserId = null
        notifications.clear()
    }

    fun isWishlisted(productId: String) = wishlist.contains(productId)

    fun toggleWishlist(productId: String) {
        val added = !wishlist.remove(productId)
        if (added) wishlist.add(productId)
        Analytics.log(Events.WISHLIST_TOGGLED, "product_id" to productId, "added" to added)
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
                        postedMinutesAgo = 3 + product.imageSeed * 17,
                        reserved = reservedMatches.contains(product.id),
                    )
                }
        }.distinctBy { it.product.id }.sortedBy { it.postedMinutesAgo }

    val unseenMatchCount: Int
        get() = notifications.count { it.kind == NotificationKind.ALERT_MATCH && !it.isRead }


    fun publishMatchNotifications() {
        alertMatches.forEach { match ->
            val isNew = notify(
                id = "match-${match.product.id}",
                title = "Alert match · ${match.alert.keyword.ifBlank { match.alert.course?.code ?: match.alert.category.label }}",
                message = "${match.product.title} — ${match.savingPercent}% below your limit.",
                kind = NotificationKind.ALERT_MATCH,
                productId = match.product.id,
            )
            if (isNew) Analytics.log(Events.MATCH_NOTIFIED, "product_id" to match.product.id, "alert_id" to match.alert.id)
        }
    }

    fun addAlert(alert: SmartAlert) {
        alerts.add(0, alert)
        Analytics.log(Events.ALERT_CREATED, "alert_id" to alert.id)
        publishMatchNotifications()
    }

    fun toggleAlert(id: String) {
        val index = alerts.indexOfFirst { it.id == id }
        if (index >= 0) alerts[index] = alerts[index].copy(enabled = !alerts[index].enabled)
    }

    fun removeAlert(id: String) {
        alerts.removeAll { it.id == id }
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

    fun ratingFor(productId: String): TransactionRating? = ratings[productId]

    fun submitRating(rating: TransactionRating) {
        ratings[rating.productId] = rating
        Analytics.log(Events.EXCHANGE_RATED, "product_id" to rating.productId)
        val product = allProducts.find { it.id == rating.productId }
        viewModelScope.launch {
            delay(2500)
            ratings[rating.productId] = rating.copy(revealed = true)
            if (product != null) {
                notify(
                    id = "rated-${rating.productId}",
                    title = "${product.seller.name.substringBefore(' ')} rated you back",
                    message = "Both ratings for \"${product.title}\" are now public.",
                    kind = NotificationKind.PRODUCT,
                    productId = product.id,
                )
            }
        }
    }

    val ratingSync = mutableStateMapOf<String, RatingResult>()

    fun sendRating(repository: RatingRepository, ratedUserId: String, rating: TransactionRating) {
        if (currentUserId == null) return
        viewModelScope.launch {
            ratingSync[rating.productId] = repository.submit(
                productId = rating.productId,
                ratedId = ratedUserId,
                stars = rating.stars,
                tags = rating.tags.map { it.label },
                review = rating.review.ifBlank { null },
            )
        }
    }
    
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

    fun loadNotifications(repository: NotificationRepository) {
        notificationRepository = repository
        val userId = currentUserId ?: return
        viewModelScope.launch {
            val feed = repository.load(userId)
            notificationsLive = feed.isLive
            notifications.removeAll { it.id in loadedNotificationIds }
            notifications.addAll(0, feed.items)
            loadedNotificationIds = feed.items.map { it.id }.toSet()
            markNotificationsRead()
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
            allProducts.removeAll { it.id in loadedMaterialIds }
            allProducts.addAll(0, feed.products)
            loadedMaterialIds = feed.products.map { it.id }.toSet()
        }
    }

    fun isRemoteNotification(id: String) = id in loadedNotificationIds

    // ---- In-app chat & meeting coordination (Views 09 / 10) ----

    private val chatThreads = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

    /** Latest meeting proposal per product thread; null until the buyer proposes one. */
    val meetingProposals = mutableStateMapOf<String, MeetingProposal>()

    private val chatStartedAt = mutableMapOf<String, Long>()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun threadFor(product: Product): SnapshotStateList<ChatMessage> =
        chatThreads.getOrPut(product.id) { mutableStateListOf<ChatMessage>().apply { addAll(SampleData.initialThread(product)) } }

    /**
     * Appends an outgoing message and simulates the Messaging Service round-trip:
     * SENDING -> SENT -> DELIVERED -> READ, followed by a canned reply from the seller.
     */
    fun sendMessage(product: Product, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val thread = threadFor(product)
        val id = "m-${System.currentTimeMillis()}"
        thread.add(ChatMessage(id, MessageAuthor.ME, trimmed, now(), MessageStatus.SENDING))
        chatStartedAt.getOrPut(product.id) { System.currentTimeMillis() }
        Analytics.log(
            Events.CHAT_MESSAGE_SENT,
            "product_id" to product.id,
            "my_messages" to thread.count { it.author == MessageAuthor.ME },
        )
        viewModelScope.launch {
            delay(500); updateStatus(thread, id, MessageStatus.SENT)
            delay(700); updateStatus(thread, id, MessageStatus.DELIVERED)
            delay(1300); updateStatus(thread, id, MessageStatus.READ)
            delay(1200)
            val reply = SampleData.cannedReplies[thread.size % SampleData.cannedReplies.size]
            thread.add(ChatMessage("$id-reply", MessageAuthor.OTHER, reply, now()))
        }
    }

    fun proposeMeeting(
        repository: MeetingProposalRepository,
        product: Product,
        point: MeetingPoint,
        slot: TimeSlot,
        onResult: (error: String?) -> Unit,
    ) {
        viewModelScope.launch {
            when (val result = repository.propose(product.id, point, slot)) {
                is ProposalResult.Success -> {
                    showProposal(product, result.proposal)
                    Analytics.log(Events.MEETING_PROPOSED, "product_id" to product.id, *chatStats(product.id, threadFor(product).size))
                    onResult(null)
                }
                ProposalResult.Offline -> onResult("No connection. Your proposal wasn't sent.")
                ProposalResult.NotSynced -> onResult("This listing or meeting point isn't on the server, so the proposal can't be sent.")
                is ProposalResult.Rejected -> onResult(result.message ?: "The server couldn't save the proposal.")
            }
        }
    }

    suspend fun refreshProposal(repository: MeetingProposalRepository, product: Product) {
        val latest = repository.latest(product.id) ?: return
        val previous = meetingProposals[product.id]
        if (latest == previous) return
        showProposal(product, latest)
        val justAccepted = latest.status == ProposalStatus.ACCEPTED &&
            (previous?.remoteId != latest.remoteId || previous?.status != ProposalStatus.ACCEPTED)
        if (justAccepted) {
            val thread = threadFor(product)
            Analytics.log(Events.MEETING_CONFIRMED, "product_id" to product.id, *chatStats(product.id, thread.size))
            thread.add(ChatMessage("confirm-${latest.remoteId}", MessageAuthor.SYSTEM, "Confirmed! See you at ${latest.point.name}, ${latest.slot.day} at ${latest.slot.label.substringBefore(' ')}.", now()))
        }
    }

    fun respondToProposal(repository: MeetingProposalRepository, product: Product, accept: Boolean, onResult: (error: String?) -> Unit) {
        val proposal = meetingProposals[product.id] ?: return
        viewModelScope.launch {
            when (val result = repository.respond(proposal, accept)) {
                is ProposalResult.Success -> {
                    refreshProposal(repository, product)
                    onResult(null)
                }
                ProposalResult.Offline -> onResult("No connection. Try again.")
                ProposalResult.NotSynced -> onResult("This proposal isn't on the server.")
                is ProposalResult.Rejected -> onResult(result.message ?: "The server couldn't save your answer.")
            }
        }
    }

    private fun showProposal(product: Product, proposal: MeetingProposal) {
        meetingProposals[product.id] = proposal
        val thread = threadFor(product)
        val text = when (proposal.status) {
            ProposalStatus.ACCEPTED -> "Meeting confirmed"
            ProposalStatus.DECLINED -> "Meeting declined"
            else -> "Meeting point proposed"
        }
        thread.removeAll { it.proposal != null }
        thread.add(ChatMessage("proposal-${proposal.remoteId}", MessageAuthor.SYSTEM, text, now(), proposal = proposal))
    }

    private fun chatStats(productId: String, messages: Int): Array<Pair<String, Any?>> = arrayOf(
        "messages_in_thread" to messages,
        "elapsed_ms" to chatStartedAt[productId]?.let { System.currentTimeMillis() - it },
    )

    private fun updateStatus(thread: SnapshotStateList<ChatMessage>, id: String, status: MessageStatus) {
        val index = thread.indexOfFirst { it.id == id }
        if (index >= 0) thread[index] = thread[index].copy(status = status)
    }

    private fun now(): String = LocalTime.now().format(timeFormatter)

    fun publishListing(draft: SellDraft): Product {
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
        )
        allProducts.add(0, product)
        Analytics.log(
            Events.LISTING_PUBLISHED,
            "category" to draft.category.name,
            "price" to product.price,
            "photo_count" to draft.photoCount,
            "course_empty" to (draft.course == null),
            "condition_empty" to (draft.condition == null),
            "description_length" to draft.description.length,
        )
        publishMatchNotifications()
        return product
    }

    init {
        publishMatchNotifications()
    }
}

data class FeaturedSeller(
    val seller: Seller,
    val itemCount: Int,
)

const val DEMO_USERNAME = "uwu"
const val DEMO_PASSWORD = "uwu123"
const val SERVICE_FEE = 2000.0
const val HOME_DELIVERY_FEE = 10000.0

data class SellDraft(
    val photoCount: Int = 0,
    val title: String = "",
    val description: String = "",
    val category: Category = Category.TEXTBOOKS,
    val price: String = "",
    val course: Course? = null,
    val notAssociatedWithCourse: Boolean = false,
    val condition: Condition? = null,
) {
    val isProductInfoValid: Boolean
        get() = photoCount > 0 &&
            InputValidation.titleError(title) == null &&
            InputValidation.descriptionError(description) == null &&
            InputValidation.priceError(price) == null

    val isCourseConditionValid: Boolean
        get() = (notAssociatedWithCourse || course != null) && condition != null
}
