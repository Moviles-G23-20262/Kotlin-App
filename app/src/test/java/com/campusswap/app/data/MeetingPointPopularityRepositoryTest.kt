package com.campusswap.app.data

import com.campusswap.app.data.remote.MeetingPointUsageDto
import com.campusswap.app.data.remote.MeetingPointUsageResponse
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class MeetingPointPopularityRepositoryTest {
    private val remote = FakePopularityRemoteDataSource()
    private val repository = MeetingPointPopularityRepository(remote)

    private fun usage(vararg totals: Pair<String, Int>) =
        MeetingPointUsageResponse(available = true, data = totals.map { MeetingPointUsageDto(it.first, it.second) })

    @Test fun keepsTheThreeBusiestPointsForTheHour() = runBlocking {
        remote.response = usage("a" to 2, "b" to 9, "c" to 5, "d" to 7)

        assertEquals(setOf("b", "d", "c"), repository.popularAt(12))
        assertEquals(listOf(12), remote.requestedHours)
    }

    @Test fun ignoresPointsWithoutExchanges() = runBlocking {
        remote.response = usage("a" to 3, "b" to 0)

        assertEquals(setOf("a"), repository.popularAt(9))
    }

    @Test fun unavailableDataHidesTheBadge() = runBlocking {
        remote.response = MeetingPointUsageResponse(available = false, data = emptyList())

        assertTrue(repository.popularAt(12).isEmpty())
    }

    @Test fun missingDataFieldHidesTheBadge() = runBlocking {
        remote.response = MeetingPointUsageResponse(available = true, data = null)

        assertTrue(repository.popularAt(12).isEmpty())
    }

    @Test fun networkOrParsingFailuresHideTheBadge() = runBlocking {
        remote.failure = IOException("offline")
        assertTrue(repository.popularAt(12).isEmpty())

        remote.failure = JsonSyntaxException("html instead of json")
        assertTrue(repository.popularAt(12).isEmpty())
    }
}
