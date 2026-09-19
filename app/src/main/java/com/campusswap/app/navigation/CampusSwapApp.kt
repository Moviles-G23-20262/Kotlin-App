package com.campusswap.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.screens.auth.LoginScreen
import com.campusswap.app.screens.cart.CartScreen
import com.campusswap.app.screens.cart.CheckoutScreen
import com.campusswap.app.screens.cart.OrderConfirmationScreen
import com.campusswap.app.screens.chat.ChatScreen
import com.campusswap.app.screens.home.AlertsScreen
import com.campusswap.app.screens.home.HomeScreen
import com.campusswap.app.screens.home.NotificationsScreen
import com.campusswap.app.screens.home.WishlistScreen
import com.campusswap.app.screens.meeting.MeetingPointScreen
import com.campusswap.app.screens.product.ProductDetailScreen
import com.campusswap.app.screens.profile.ProfileScreen
import com.campusswap.app.screens.rating.CompletionScreen
import com.campusswap.app.screens.search.SearchScreen
import com.campusswap.app.screens.sell.SellScreen
import com.campusswap.app.ui.theme.CampusSwapTheme

@Composable
fun CampusSwapApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // As in the prototype, the bottom bar is hidden on Login and on the order confirmation,
    // plus the focused full-screen tasks (Sell flow, Chat, Meeting point).
    val hideChrome = currentRoute == null ||
        currentRoute == Routes.LOGIN ||
        currentRoute == Routes.CONFIRMATION ||
        currentRoute == Routes.SELL ||
        currentRoute == Routes.CHAT ||
        currentRoute == Routes.MEETING_POINT ||
        currentRoute == Routes.COMPLETION

    fun navigateToTab(route: String) {
        if (route == Routes.SELL) {
            navController.navigate(route)
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        containerColor = CampusSwapTheme.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!hideChrome) {
                CampusSwapBottomBar(
                    currentRoute = currentRoute,
                    cartCount = appViewModel.cart.sumOf { it.quantity },
                    onNavigate = ::navigateToTab,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            composable(Routes.LOGIN) {
                // login screen keeps the  original design
                Box(Modifier.systemBarsPadding()) {
                LoginScreen(
                    onLoginSuccess = {
                        appViewModel.login()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
                }
            }

            composable(Routes.HOME) {
                HomeScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onSeeAllCategory = { category ->
                        appViewModel.pendingSearchCategory = category
                        navigateToTab(Routes.SEARCH)
                    },
                    onSearchClick = { navigateToTab(Routes.SEARCH) },
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onWishlistClick = { navController.navigate(Routes.WISHLIST) },
                )
            }

            composable(Routes.SEARCH) {
                val initialCategory = appViewModel.pendingSearchCategory
                appViewModel.pendingSearchCategory = null
                SearchScreen(
                    vm = appViewModel,
                    initialCategory = initialCategory,
                    onBack = { navigateToTab(Routes.HOME) },
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                )
            }

            composable(Routes.SELL) {
                SellScreen(
                    vm = appViewModel,
                    onExit = {
                        if (!navController.popBackStack()) navigateToTab(Routes.HOME)
                    },
                    onPublished = { id ->
                        navController.navigate(Routes.productDetail(id)) {
                            popUpTo(Routes.HOME)
                        }
                    },
                )
            }

            composable(Routes.CART) {
                CartScreen(
                    vm = appViewModel,
                    onBack = { navigateToTab(Routes.HOME) },
                    onBrowse = { navigateToTab(Routes.SEARCH) },
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onCheckout = { navController.navigate(Routes.CHECKOUT) },
                )
            }

            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    vm = appViewModel,
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = { orderNumber ->
                        navController.navigate(Routes.confirmation(orderNumber)) {
                            popUpTo(Routes.CART) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.CONFIRMATION) { entry ->
                val orderNumber = entry.arguments?.getString("orderNumber").orEmpty()
                OrderConfirmationScreen(
                    orderNumber = orderNumber,
                    order = appViewModel.orders.firstOrNull { it.number.toString() == orderNumber },
                    onBackToHome = { navigateToTab(Routes.HOME) },
                    onKeepShopping = { navigateToTab(Routes.SEARCH) },
                    onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                    onCompleteExchange = { id -> navController.navigate(Routes.completion(id)) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onAddItem = { navController.navigate(Routes.SELL) },
                    onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                    onCompleteExchange = { id -> navController.navigate(Routes.completion(id)) },
                    onLogout = {
                        appViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.PRODUCT_DETAIL) { backStackEntryArgs ->
                val productId = backStackEntryArgs.arguments?.getString("productId").orEmpty()
                ProductDetailScreen(
                    vm = appViewModel,
                    productId = productId,
                    onBack = { if (!navController.popBackStack()) navigateToTab(Routes.HOME) },
                    onRelatedClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onChatWithSeller = { id -> navController.navigate(Routes.chat(id)) },
                    onBuyNow = { navigateToTab(Routes.CART) },
                    onCompleteExchange = { id -> navController.navigate(Routes.completion(id)) },
                )
            }

            composable(Routes.CHAT) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                ChatScreen(
                    vm = appViewModel,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onViewListing = { id -> navController.navigate(Routes.productDetail(id)) },
                    onProposeMeeting = { navController.navigate(Routes.meetingPoint(productId)) },
                    onCompleteExchange = { navController.navigate(Routes.completion(productId)) },
                )
            }

            composable(Routes.MEETING_POINT) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                MeetingPointScreen(
                    vm = appViewModel,
                    productId = productId,
                    onBack = { navController.popBackStack() },
                    onProposed = { navController.popBackStack() },
                )
            }

            composable(Routes.COMPLETION) { entry ->
                CompletionScreen(
                    vm = appViewModel,
                    productId = entry.arguments?.getString("productId").orEmpty(),
                    onClose = { if (!navController.popBackStack()) navigateToTab(Routes.HOME) },
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    vm = appViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenAlerts = { navController.navigate(Routes.ALERTS) },
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                    onCompleteExchange = { id -> navController.navigate(Routes.completion(id)) },
                )
            }

            composable(Routes.WISHLIST) {
                WishlistScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onOpenAlerts = { navController.navigate(Routes.ALERTS) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ALERTS) {
                AlertsScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
