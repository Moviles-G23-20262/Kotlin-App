package com.campusswap.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.campusswap.app.components.CampusIcons

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SELL = "sell"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val CONFIRMATION = "confirmation/{orderNumber}"
    const val PROFILE = "profile"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val NOTIFICATIONS = "notifications"
    const val WISHLIST = "wishlist"
    const val ALERTS = "alerts"
    const val CHAT = "chat/{productId}"
    const val MEETING_POINT = "meeting/{productId}"
    const val COMPLETION = "complete/{productId}"

    fun productDetail(id: String) = "product/$id"
    fun chat(id: String) = "chat/$id"
    fun meetingPoint(id: String) = "meeting/$id"
    fun completion(id: String) = "complete/$id"
    fun confirmation(orderNumber: Int) = "confirmation/$orderNumber"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isCentral: Boolean = false,
    val alsoActiveOn: Set<String> = emptySet(),
)

val bottomNavItems: List<BottomNavItem>
    get() = listOf(
        BottomNavItem(Routes.HOME, "Home", CampusIcons.Home),
        BottomNavItem(Routes.SEARCH, "Search", CampusIcons.Search),
        BottomNavItem(Routes.SELL, "Sell", CampusIcons.Plus, isCentral = true),
        BottomNavItem(Routes.CART, "Cart", CampusIcons.Cart, alsoActiveOn = setOf(Routes.CHECKOUT)),
        BottomNavItem(Routes.PROFILE, "Seller", CampusIcons.User),
    )
