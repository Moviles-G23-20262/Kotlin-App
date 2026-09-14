package com.campusswap.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.campusswap.app.screens.chat.ChatScreen
import com.campusswap.app.screens.meeting.MeetingPointScreen
import com.campusswap.app.screens.home.HomeScreen
import com.campusswap.app.screens.home.NotificationsScreen
import com.campusswap.app.screens.home.WishlistScreen
import com.campusswap.app.screens.product.ProductDetailScreen
import com.campusswap.app.screens.profile.ProfileScreen
import com.campusswap.app.screens.search.SearchScreen
import com.campusswap.app.screens.sell.SellScreen

@Composable
fun CampusSwapApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Focused, full-screen tasks hide the bottom bar (Section 2.5 "Transient" layer).
    val hideChrome = currentRoute == null ||
        currentRoute == Routes.LOGIN ||
        currentRoute == Routes.SELL ||
        currentRoute == Routes.CHAT ||
        currentRoute == Routes.MEETING_POINT

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
        bottomBar = {
            if (!hideChrome) {
                CampusSwapBottomBar(currentRoute = currentRoute, onNavigate = ::navigateToTab)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        appViewModel.login()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
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
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                )
            }

            composable(Routes.SELL) {
                SellScreen(
                    vm = appViewModel,
                    onExit = {
                        navController.popBackStack(Routes.HOME, inclusive = false)
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
                    onBrowse = { navigateToTab(Routes.HOME) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
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
                    onBack = { navController.popBackStack() },
                    onRelatedClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onChatWithSeller = { id -> navController.navigate(Routes.chat(id)) },
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

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(vm = appViewModel, onBack = { navController.popBackStack() })
            }

            composable(Routes.WISHLIST) {
                WishlistScreen(
                    vm = appViewModel,
                    onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
