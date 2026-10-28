package com.campusswap.app.data

import com.campusswap.app.data.remote.ExchangeDto
import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ExchangeRepositoryTest {
    private val remote = FakeExchangeRemoteDataSource()
    private val repository = ExchangeRepository(remote)
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val target = ExchangeTarget("p1", "me", "s1", 320000.0, "mp1", GeoPoint(10.0, 20.0))
    private val material = "b0000000-0000-4000-8000-000000000001"

    @Test fun completesTheBuyersPendingOrderWithTheMeasuredLocation() = runBlocking {
        remote.exchanges += ExchangeDto("pending-1", me, "PENDING")

        val result = repository.confirm(target, GeoPoint(10.0001, 20.0002))

        assertEquals(ConfirmResult.Success, result)
        assertTrue(remote.orders.isEmpty())
        assertEquals(Completion("pending-1", 10.0001, 20.0002), remote.completions.single())
    }

    @Test fun placesTheOrderFirstWhenThereIsNoneYet() = runBlocking {
        repository.confirm(target, GeoPoint(10.0, 20.0))

        assertEquals(listOf(material), remote.orders)
        assertEquals("order-1", remote.completions.single().exchangeId)
    }

    @Test fun manualCheckInCompletesWithoutCoordinates() = runBlocking {
        remote.exchanges += ExchangeDto("pending-1", me, "PENDING")

        repository.confirm(target, null)

        assertEquals(Completion("pending-1", null, null), remote.completions.single())
    }

    @Test fun anExchangeAlreadyCompletedIsNotRecordedAgain() = runBlocking {
        remote.exchanges += ExchangeDto("done-1", me, "COMPLETED")

        assertEquals(ConfirmResult.Success, repository.confirm(target, null))
        assertTrue(remote.orders.isEmpty())
        assertTrue(remote.completions.isEmpty())
        assertTrue(repository.isConfirmed("p1"))
    }

    @Test fun anotherBuyersOrderIsIgnored() = runBlocking {
        remote.exchanges += ExchangeDto("someone-else", "a0000000-0000-4000-8000-000000000009", "PENDING")

        repository.confirm(target, null)

        assertEquals(listOf(material), remote.orders)
    }

    @Test fun meetingPointLoadedFromTheApiIsNotNeededToComplete() = runBlocking {
        remote.exchanges += ExchangeDto("pending-1", me, "PENDING")

        assertEquals(ConfirmResult.Success, repository.confirm(target.copy(meetingPointId = "c-uuid"), null))
    }

    @Test fun locallyPublishedListingIsNotSent() = runBlocking {
        assertEquals(ConfirmResult.NotSynced, repository.confirm(target.copy(productId = "local-13"), null))
        assertTrue(remote.orders.isEmpty())
    }

    @Test fun offlineFailureIsNotRememberedSoRetryWorks() = runBlocking {
        remote.failure = IOException("airplane mode")
        assertEquals(ConfirmResult.Offline, repository.confirm(target, null))

        remote.failure = null
        assertEquals(ConfirmResult.Success, repository.confirm(target, null))
        assertEquals(1, remote.completions.size)
    }

    @Test fun serverRejectionReportsTheHttpCode() = runBlocking {
        remote.failure = HttpException(Response.error<Any>(409, ResponseBody.create(null, "")))

        assertEquals(ConfirmResult.Rejected(409), repository.confirm(target, null))
    }

    @Test fun confirmedExchangeIsNotPostedTwiceInTheSameSession() = runBlocking {
        repository.confirm(target, null)
        repository.confirm(target, null)

        assertEquals(1, remote.completions.size)
    }
}
