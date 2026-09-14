package com.campusswap.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

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
