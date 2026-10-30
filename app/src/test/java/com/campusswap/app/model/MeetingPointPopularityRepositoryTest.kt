package com.campusswap.app.model

import com.campusswap.app.model.local.InMemoryResponseCache
import com.campusswap.app.model.remote.MeetingPointUsageDto
import com.campusswap.app.model.remote.MeetingPointUsageResponse
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MeetingPointPopularityRepositoryTest {
    private val remote = FakePopularityRemoteDataSource()
    private val cache = InMemoryResponseCache(now = 5_000L)
    private val repository = MeetingPointPopularityRepository(remote, cache)

    private fun usage(vararg totals: Pair<String, Int>) =
        MeetingPointUsageResponse(available = true, data = totals.map { MeetingPointUsageDto(it.first, it.second) })

    @Test fun keepsTheThreeBusiestPointsForTheHour() = runBlocking {
        remote.response = usage("a" to 2, "b" to 9, "c" to 5, "d" to 7)

        val result = repository.popularAt(12)

        assertEquals(setOf("b", "d", "c"), result.ids)
        assertTrue(result.isLive)
        assertNull(result.savedAtMillis)
        assertEquals(listOf(12), remote.requestedHours)
    }

    @Test fun ignoresPointsWithoutExchanges() = runBlocking {
        remote.response = usage("a" to 3, "b" to 0)

        assertEquals(setOf("a"), repository.popularAt(9).ids)
    }

    @Test fun unavailableDataHidesTheBadge() = runBlocking {
        remote.response = MeetingPointUsageResponse(available = false, data = emptyList())

        assertTrue(repository.popularAt(12).ids.isEmpty())
    }

    @Test fun missingDataFieldHidesTheBadge() = runBlocking {
        remote.response = MeetingPointUsageResponse(available = true, data = null)

        assertTrue(repository.popularAt(12).ids.isEmpty())
    }

    @Test fun savesTheAnswerForTheHour() = runBlocking {
        remote.response = usage("a" to 4)
        repository.popularAt(12)

        assertTrue(cache.get("*", "meeting_point_usage:12") != null)
    }

    @Test fun offlineUsesTheCopySavedForTheSameHour() = runBlocking {
        remote.response = usage("a" to 4, "b" to 1)
        repository.popularAt(12)

        remote.failure = IOException("airplane mode")
        val afterRestart = MeetingPointPopularityRepository(remote, cache).popularAt(12)

        assertEquals(setOf("a", "b"), afterRestart.ids)
        assertFalse(afterRestart.isLive)
        assertEquals(5_000L, afterRestart.savedAtMillis)
    }

    @Test fun offlineDoesNotUseTheCopyOfAnotherHour() = runBlocking {
        remote.response = usage("a" to 4)
        repository.popularAt(12)

        remote.failure = IOException("airplane mode")
        val result = repository.popularAt(13)

        assertTrue(result.ids.isEmpty())
        assertNull(result.savedAtMillis)
    }

    @Test fun networkOrParsingFailuresWithoutACopyHideTheBadge() = runBlocking {
        remote.failure = IOException("offline")
        assertTrue(repository.popularAt(12).ids.isEmpty())

        remote.failure = JsonSyntaxException("html instead of json")
        assertTrue(repository.popularAt(12).ids.isEmpty())
    }

    @Test fun unavailableDataKeepsTheSavedCopy() = runBlocking {
        remote.response = usage("a" to 4)
        repository.popularAt(12)
        remote.response = MeetingPointUsageResponse(available = false, data = emptyList())
        repository.popularAt(12)

        remote.failure = IOException("airplane mode")

        assertEquals(setOf("a"), repository.popularAt(12).ids)
    }

    @Test fun cancellationIsNotTreatedAsOffline() {
        remote.failure = CancellationException("screen closed")

        assertThrows(CancellationException::class.java) { runBlocking { repository.popularAt(12) } }
    }
}
