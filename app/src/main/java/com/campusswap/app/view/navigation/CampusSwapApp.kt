package com.campusswap.app.view.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusswap.app.model.analytics.Analytics
import com.campusswap.app.model.analytics.Events
import com.campusswap.app.viewmodel.AppViewModel
import com.campusswap.app.model.auth.SessionManager
import com.campusswap.app.model.auth.SessionState
import com.campusswap.app.view.screens.auth.LoginScreen
import com.campusswap.app.view.screens.auth.RegisterScreen
import com.campusswap.app.view.screens.cart.CartScreen
import com.campusswap.app.view.screens.cart.CheckoutScreen
import com.campusswap.app.view.screens.cart.OrderConfirmationScreen
import com.campusswap.app.view.screens.chat.ChatScreen
import com.campusswap.app.view.screens.chat.MessagesScreen
import com.campusswap.app.view.screens.home.AlertsScreen
import com.campusswap.app.view.screens.home.HomeScreen
import com.campusswap.app.view.screens.home.NotificationsScreen
import com.campusswap.app.view.screens.home.WishlistScreen
import com.campusswap.app.view.screens.meeting.MeetingPointScreen
import com.campusswap.app.view.screens.product.ProductDetailScreen
import com.campusswap.app.view.screens.profile.ProfileScreen
import com.campusswap.app.view.screens.rating.CompletionScreen
import com.campusswap.app.view.screens.search.SearchScreen
import com.campusswap.app.view.screens.sell.SellScreen
import com.campusswap.app.view.theme.CampusSwapTheme
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.campusswap.app.CampusSwapApplication

@Composable
fun CampusSwapApp(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val container = (LocalContext.current.applicationContext as CampusSwapApplication).container
    val session by container.sessionManager.state.collectAsState()
    if (session == SessionState.Restoring) {
        Box(Modifier.fillMaxSize().background(CampusSwapTheme.colors.bg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val startDestination = remember { if (session is SessionState.SignedIn) Routes.HOME else Routes.LOGIN }

    val context = LocalContext.current

    // Everything the last account left behind goes: its data, the tabs it left open and its
    // notifications in the shade, so the next log-in (or demo access) starts clean.
    fun clearUserAndGoToLogin() {
        appViewModel.logout()
        NotificationManagerCompat.from(context).cancelAll()
        bottomNavItems.forEach { navController.clearBackStack(it.route) }
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(session) {
        when (val current = session) {
            is SessionState.SignedIn -> {
                appViewModel.useSession(current.session)
                if (!appViewModel.isLoggedIn) appViewModel.restoreSession(current.session.userId)
            }
            is SessionState.SignedOut -> if (current.expired) clearUserAndGoToLogin()
            SessionState.Restoring -> Unit
        }
    }

    LaunchedEffect(Unit) {
        appViewModel.startOfflineSync(
            connectivity = container.connectivity,
            outbox = container.outboxRepository,
            materials = container.materialRepository,
            sessionUserId = { (container.sessionManager.state.value as? SessionState.SignedIn)?.session?.userId },
        )
    }

    LaunchedEffect(appViewModel.isLoggedIn) {
        // Demo access has no token: the server would answer 401 and Home would wrongly look offline.
        if (appViewModel.isLoggedIn && container.sessionManager.token != null) {
            appViewModel.loadMaterials(container.materialRepository)
            appViewModel.loadWishlist(container.wishlistRepository)
            appViewModel.loadOrders(container.orderRepository)
            appViewModel.restoreCart(container.cartStore)
            appViewModel.loadMyReviews(container.ratingRepository)
            appViewModel.loadAlerts(container.smartAlertRepository)
            appViewModel.loadNotifications(container.notificationRepository, markRead = false)
            // Anything queued before a log-out or an expired session goes out with the new one.
            container.outboxRepository.syncNow()
        }
    }

    var unreadMessages by remember { mutableIntStateOf(0) }
    LaunchedEffect(appViewModel.isLoggedIn, currentRoute) {
        while (appViewModel.isLoggedIn) {
            unreadMessages = container.conversationsRepository.unreadCount()
            delay(UNREAD_POLL_MS)
        }
    }

    LaunchedEffect(backStackEntry) {
        val route = currentRoute ?: return@LaunchedEffect
        Analytics.screen(route)
        if (route == Routes.PRODUCT_DETAIL) {
            val id = backStackEntry?.arguments?.getString("productId")
            val product = id?.let(appViewModel::findProduct)
            Analytics.log(Events.LISTING_OPENED, "product_id" to id, "category" to product?.category?.name)
        }
    }

    // As in the prototype, the bottom bar is hidden on Login and on the order confirmation,
    // plus the focused full-screen tasks (Chat, Meeting point).
    val hideChrome = currentRoute == null ||
        currentRoute == Routes.LOGIN ||
        currentRoute == Routes.REGISTER ||
        currentRoute == Routes.CONFIRMATION ||
        currentRoute == Routes.CHAT ||
        currentRoute == Routes.MEETING_POINT ||
        currentRoute == Routes.COMPLETION

    // Each tab keeps its own stack like iOS: leaving a tab saves it and coming back restores it
    // where it was. Tapping a tab that is already open (e.g. from a detail on top of it) goes to its root.
    fun navigateToTab(route: String) {
        val inStack = runCatching { navController.getBackStackEntry(route) }.isSuccess
        if (inStack && route != Routes.HOME) {
            navController.popBackStack(route, inclusive = false)
            return
        }
        navController.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Favoritos marcados sin red o rechazados: se puede tocar el corazón desde cualquier pantalla
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(appViewModel.wishlistMessage) {
        val message = appViewModel.wishlistMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        appViewModel.clearWishlistMessage()
    }

    Scaffold(
        containerColor = CampusSwapTheme.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            composable(Routes.LOGIN) {
                // login screen keeps the  original design
                Box(Modifier.systemBarsPadding()) {
                LoginScreen(
                    onLoginSuccess = { userId ->
                        appViewModel.login(userId)
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onCreateAccount = { navController.navigate(Routes.REGISTER) { launchSingleTop = true } },
                    sessionExpired = (session as? SessionState.SignedOut)?.expired == true,
                )
                }
            }

            composable(Routes.REGISTER) {
                Box(Modifier.systemBarsPadding()) {
                    RegisterScreen(
                        onRegistered = { userId ->
                            appViewModel.login(userId)
                            navController.navigate(Routes.HOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onBackToLogin = { navController.popBackStack() },
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
                    onMessagesClick = { navController.navigate(Routes.MESSAGES) },
                    unreadMessages = unreadMessages,
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
                    onOpenChat = { id, room -> navController.navigate(Routes.chat(id, room)) },
                    onCompleteExchange = { id -> navController.navigate(Routes.completion(id)) },
                    onLogout = {
                        container.authRepository.logout()
                        clearUserAndGoToLogin()
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

            composable(Routes.MESSAGES) {
                MessagesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { id, room -> navController.navigate(Routes.chat(id, room)) },
                )
            }

            composable(
                Routes.CHAT,
                arguments = listOf(navArgument("room") { type = NavType.StringType; nullable = true }),
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                val room = entry.arguments?.getString("room")
                RequireSession(navController, container.sessionManager) {
                    ChatScreen(
                        vm = appViewModel,
                        productId = productId,
                        room = room,
                        onBack = { navController.popBackStack() },
                        onViewListing = { id -> navController.navigate(Routes.productDetail(id)) },
                        onProposeMeeting = { navController.navigate(Routes.meetingPoint(productId)) },
                        onCompleteExchange = { navController.navigate(Routes.completion(productId)) },
                    )
                }
            }

            composable(Routes.MEETING_POINT) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                RequireSession(navController, container.sessionManager) {
                    MeetingPointScreen(
                        vm = appViewModel,
                        productId = productId,
                        onBack = { navController.popBackStack() },
                        onProposed = { navController.popBackStack() },
                    )
                }
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

@Composable
private fun RequireSession(navController: NavHostController, sessions: SessionManager, content: @Composable () -> Unit) {
    val session = remember { sessions.validSession() }
    if (session != null) {
        content()
        return
    }
    LaunchedEffect(Unit) {
        navController.navigate(Routes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
}

private const val UNREAD_POLL_MS = 15_000L
