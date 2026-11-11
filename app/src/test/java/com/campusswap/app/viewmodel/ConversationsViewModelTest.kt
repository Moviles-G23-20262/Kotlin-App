package com.campusswap.app.viewmodel

import com.campusswap.app.model.ConversationsRepository
import com.campusswap.app.model.FakeChatRoomRemoteDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ConversationsViewModelTest {
    private val remote = FakeChatRoomRemoteDataSource()
    private val online = MutableStateFlow(true)
    private val vm = ConversationsViewModel(ConversationsRepository(remote, currentUserId = { "me" }), online)

    @Test
    fun `polls every 10 seconds while online`() = runTest {
        val poll = launch { vm.pollWhileVisible() }
        runCurrent()
        advanceTimeBy(ConversationsViewModel.POLL_INTERVAL_MS * 3)
        runCurrent()
        poll.cancel()

        assertEquals(4, remote.roomsCalls)
    }

    @Test
    fun `offline it asks once and then waits for the network`() = runTest {
        online.value = false
        remote.failure = IOException("offline")
        val poll = launch { vm.pollWhileVisible() }
        runCurrent()
        advanceTimeBy(ConversationsViewModel.POLL_INTERVAL_MS * 30)
        runCurrent()

        assertEquals(1, remote.roomsCalls)

        remote.failure = null
        online.value = true
        runCurrent()
        poll.cancel()

        assertEquals(2, remote.roomsCalls)
    }

    @Test
    fun `nothing is requested while the screen is not visible`() = runTest {
        // The view model no longer polls on its own: only the screen's WhileVisible starts it.
        advanceTimeBy(ConversationsViewModel.POLL_INTERVAL_MS * 10)
        assertEquals(0, remote.roomsCalls)
    }
}
