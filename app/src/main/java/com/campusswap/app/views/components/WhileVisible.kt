package com.campusswap.app.views.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope

/**
 * Runs [block] only while this screen is on screen (lifecycle STARTED) and cancels it as soon as the
 * user moves to another screen or tab, or the app goes to the background; it starts again when the
 * screen comes back. Inside a navigation destination the lifecycle is the back stack entry's, so a
 * screen left in the back stack (another tab, a chat under the meeting point screen) also stops.
 *
 * Used for polling: a loop in `viewModelScope` or a plain `LaunchedEffect` keeps running for screens
 * that are not visible, spending battery and mobile data on answers nobody sees.
 */
@Composable
fun WhileVisible(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, *keys) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED, block)
    }
}
