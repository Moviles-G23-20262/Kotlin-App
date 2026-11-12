package com.campusswap.app.viewmodel

import com.campusswap.app.model.FakeChatRoomRemoteDataSource
import com.campusswap.app.model.FakeMeetingProposalRemoteDataSource
import com.campusswap.app.model.MeetupsRepository
import com.campusswap.app.model.local.FakeMeetupDao
import com.campusswap.app.model.local.MeetupEntity
import com.campusswap.app.model.remote.ChatRoomSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class MeetupsViewModelTest {
    private val rooms = FakeChatRoomRemoteDataSource()
    private val dao = FakeMeetupDao()
    private val online = MutableStateFlow(true)
    private val now = Instant.parse("2026-11-12T15:00:00Z")
    private val repository = MeetupsRepository(
        rooms, FakeMeetingProposalRemoteDataSource(), dao, { "me" }, Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun saved(id: String) = MeetupEntity(
        ownerId = "me",
        proposalId = id,
        roomId = "room-1",
        productId = "material-1",
        productTitle = "Calculus book",
        counterpartName = "Ana Ruiz",
        pointName = "Central Library lobby",
        startsAtMillis = now.plusSeconds(3600).toEpochMilli(),
        endsAtMillis = now.plusSeconds(7200).toEpochMilli(),
        confirmed = true,
        proposedByMe = false,
        savedAtMillis = now.minusSeconds(600).toEpochMilli(),
    )

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the saved agenda shows up before the network answers`() {
        dao.rows += saved("p1")

        val state = MeetupsViewModel(repository, online).state.value

        assertFalse(state.isLoading)
        assertEquals(listOf("p1"), state.meetups.map { it.proposalId })
        assertNull(state.savedAtMillis)
    }

    @Test
    fun `offline it keeps the saved agenda and says how old it is`() = runTest {
        dao.rows += saved("p1")
        rooms.failure = IOException("airplane mode")
        val vm = MeetupsViewModel(repository, online)

        vm.refresh()

        assertEquals(listOf("p1"), vm.state.value.meetups.map { it.proposalId })
        assertEquals(now.minusSeconds(600).toEpochMilli(), vm.state.value.savedAtMillis)
    }

    @Test
    fun `offline with nothing saved it shows the offline state`() = runTest {
        rooms.failure = IOException("airplane mode")
        val vm = MeetupsViewModel(repository, online)

        vm.refresh()

        assertTrue(vm.state.value.isOffline)
    }

    @Test
    fun `it refreshes when the screen shows up and again when the network comes back`() = runTest {
        online.value = false
        val vm = MeetupsViewModel(repository, online)
        val visible = launch { vm.refreshWhileVisible() }
        runCurrent()
        assertEquals(1, rooms.roomsCalls)

        online.value = true
        runCurrent()
        visible.cancel()

        assertEquals(2, rooms.roomsCalls)
    }

    @Test
    fun `a live answer replaces the saved agenda`() = runTest {
        dao.rows += saved("p1")
        rooms.summaries += ChatRoomSummaryDto("room-1", "material-1", "me", "ana", null, null, null, null, null)
        val vm = MeetupsViewModel(repository, online)

        vm.refresh()

        assertTrue(vm.state.value.meetups.isEmpty())
        assertNull(vm.state.value.savedAtMillis)
    }
}
