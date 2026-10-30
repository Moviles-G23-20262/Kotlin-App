package com.campusswap.app.views.navigation

import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import com.campusswap.app.views.components.CampusIcons

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password?email={email}"
    const val VERIFY_EMAIL = "verify-email"
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
    const val MESSAGES = "messages"
    const val CHAT = "chat/{productId}?room={room}"
    const val MEETING_POINT = "meeting/{productId}"
    const val COMPLETION = "complete/{productId}"

    fun forgotPassword(email: String) = "forgot-password?email=${Uri.encode(email)}"
    fun productDetail(id: String) = "product/$id"
    fun chat(id: String, room: String? = null) = if (room == null) "chat/$id" else "chat/$id?room=$room"
    fun meetingPoint(id: String) = "meeting/$id"
    fun completion(id: String) = "complete/$id"
    fun confirmation(orderNumber: Int) = "confirmation/$orderNumber"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val isCentral: Boolean = false,
    val alsoActiveOn: Set<String> = emptySet(),
)

val bottomNavItems: List<BottomNavItem>
    get() = listOf(
        BottomNavItem(Routes.HOME, "Home", CampusIcons.Home, CampusIcons.HomeFilled),
        BottomNavItem(Routes.SEARCH, "Search", CampusIcons.Search, CampusIcons.SearchFilled),
        BottomNavItem(Routes.SELL, "Sell", CampusIcons.Plus, isCentral = true),
        BottomNavItem(Routes.CART, "Cart", CampusIcons.Cart, CampusIcons.CartFilled, alsoActiveOn = setOf(Routes.CHECKOUT)),
        BottomNavItem(Routes.PROFILE, "Seller", CampusIcons.User, CampusIcons.UserFilled),
    )
