package com.campusswap.app.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConnectivityObserver {
    /** True while the device has a network that can reach the internet. */
    val isOnline: StateFlow<Boolean>
}

/**
 * Listens to the system's default network. The callback runs on a ConnectivityManager thread;
 * writing to a StateFlow is thread-safe, and the UI collects it on the main thread.
 */
class NetworkConnectivityObserver(context: Context) : ConnectivityObserver {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(currentlyOnline())
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state.value = capabilities.hasInternet()
            }

            override fun onLost(network: Network) {
                state.value = false
            }
        })
    }

    private fun currentlyOnline(): Boolean =
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasInternet() == true

    private fun NetworkCapabilities.hasInternet() =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
