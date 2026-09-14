package com.campusswap.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Single in-memory state holder for the whole prototype. There is no backend:
 * every action here just mutates local Compose state so the UI behaves consistently
 * across screens (cart badge, wishlist hearts, newly published listings, etc.).
 */
class AppViewModel : ViewModel() {

    var isDarkTheme by mutableStateOf(false)
        private set

    var isLoggedIn by mutableStateOf(false)
        private set

    val currentUser = SampleData.currentSeller

    val allProducts = mutableStateListOf<Product>().apply { addAll(SampleData.products) }

    val cart = mutableStateListOf<CartLine>()

    val wishlist = mutableStateListOf<String>()

    val notifications = mutableStateListOf<AppNotification>().apply { addAll(SampleData.notifications) }

    /** Set by Home's "See all" before navigating to Search, then consumed once. */
    var pendingSearchCategory: Category? = null

    val myListings get() = allProducts.filter { it.seller.id == currentUser.id }

    val cartTotal get() = cart.sumOf { it.product.price * it.quantity }

    val unreadNotificationCount get() = notifications.count { !it.isRead }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }

    fun login() {
        isLoggedIn = true
    }

    fun logout() {
        isLoggedIn = false
        cart.clear()
    }

    fun isWishlisted(productId: String) = wishlist.contains(productId)

    fun toggleWishlist(productId: String) {
        if (!wishlist.remove(productId)) wishlist.add(productId)
    }

    fun addToCart(product: Product) {
        val index = cart.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = cart[index]
            cart[index] = existing.copy(quantity = existing.quantity + 1)
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
            cart[index] = cart[index].copy(quantity = quantity)
        }
    }

    fun markNotificationsRead() {
        for (i in notifications.indices) {
            notifications[i] = notifications[i].copy(isRead = true)
        }
    }

    // ---- In-app chat & meeting coordination (Views 09 / 10) ----

    private val chatThreads = mutableStateMapOf<String, SnapshotStateList<ChatMessage>>()

    /** Latest meeting proposal per product thread; null until the buyer proposes one. */
    val meetingProposals = mutableStateMapOf<String, MeetingProposal>()

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
        viewModelScope.launch {
            delay(500); updateStatus(thread, id, MessageStatus.SENT)
            delay(700); updateStatus(thread, id, MessageStatus.DELIVERED)
            delay(1300); updateStatus(thread, id, MessageStatus.READ)
            delay(1200)
            val reply = SampleData.cannedReplies[thread.size % SampleData.cannedReplies.size]
            thread.add(ChatMessage("$id-reply", MessageAuthor.OTHER, reply, now()))
        }
    }

    /** Recommended point = lowest combined walk time that is a monitored zone, preferring the equidistant one. */
    fun recommendedMeetingPoint(): MeetingPoint =
        SampleData.meetingPoints
            .filter { it.isMonitored }
            .minBy { (it.walkMinutesMe + it.walkMinutesOther) * 10 + kotlin.math.abs(it.walkMinutesMe - it.walkMinutesOther) }

    fun recommendedTimeSlot(): TimeSlot = SampleData.timeSlots.first { it.isSharedBreak }

    fun proposeMeeting(product: Product, point: MeetingPoint, slot: TimeSlot) {
        val proposal = MeetingProposal(point, slot, ProposalStatus.PENDING)
        meetingProposals[product.id] = proposal
        val thread = threadFor(product)
        thread.removeAll { it.proposal != null }
        thread.add(ChatMessage("proposal-${System.currentTimeMillis()}", MessageAuthor.SYSTEM, "Meeting point proposed", now(), proposal = proposal))
        viewModelScope.launch {
            delay(2500)
            val current = meetingProposals[product.id] ?: return@launch
            if (current.status == ProposalStatus.PENDING) {
                val accepted = current.copy(status = ProposalStatus.ACCEPTED)
                meetingProposals[product.id] = accepted
                val index = thread.indexOfLast { it.proposal != null }
                if (index >= 0) thread[index] = thread[index].copy(text = "Meeting confirmed", proposal = accepted)
                thread.add(ChatMessage("confirm-${System.currentTimeMillis()}", MessageAuthor.OTHER, "Confirmed! See you at ${current.point.name} at ${current.slot.label.substringBefore(' ')}.", now()))
            }
        }
    }

    private fun updateStatus(thread: SnapshotStateList<ChatMessage>, id: String, status: MessageStatus) {
        val index = thread.indexOfFirst { it.id == id }
        if (index >= 0) thread[index] = thread[index].copy(status = status)
    }

    private fun now(): String = LocalTime.now().format(timeFormatter)

    fun publishListing(draft: SellDraft): Product {
        val product = Product(
            id = "local-${allProducts.size + 1}",
            title = draft.title,
            description = draft.description,
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
        return product
    }
}

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
        get() = title.isNotBlank() && description.isNotBlank() && price.toDoubleOrNull() != null && photoCount > 0

    val isCourseConditionValid: Boolean
        get() = (notAssociatedWithCourse || course != null) && condition != null
}
