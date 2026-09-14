package com.campusswap.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SEARCH = "search"
    const val SELL = "sell"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val NOTIFICATIONS = "notifications"
    const val WISHLIST = "wishlist"
    const val CHAT = "chat/{productId}"
    const val MEETING_POINT = "meeting/{productId}"

    fun productDetail(id: String) = "product/$id"
    fun chat(id: String) = "chat/$id"
    fun meetingPoint(id: String) = "meeting/$id"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
    val isCentral: Boolean = false,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.SELL, "Sell", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, isCentral = true),
    BottomNavItem(Routes.CART, "Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomNavItem(Routes.PROFILE, "Seller", Icons.Filled.Person, Icons.Outlined.Person),
)
