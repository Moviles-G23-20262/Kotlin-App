package com.campusswap.app.model

import com.campusswap.app.model.remote.ExchangeDto
import com.campusswap.app.model.domain.ExchangeTarget
import com.campusswap.app.model.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ExchangeRepositoryTest {
    private val remote = FakeExchangeRemoteDataSource()
    private val repository = ExchangeRepository(remote)
    private val me = "a0000000-0000-4000-8000-000000000000"
    private val seller = "a0000000-0000-4000-8000-000000000005"
    private val target = ExchangeTarget("p1", "me", "s1", 320000.0, "mp1", GeoPoint(10.0, 20.0))
    private val sellerTarget = ExchangeTarget("p1", me, seller, 320000.0, "mp1", null, asSeller = true)
    private val material = "b0000000-0000-4000-8000-000000000001"

    private fun pendingOrder(id: String = "pending-1", buyerConfirmedAt: String? = null, sellerConfirmedAt: String? = null) =
        ExchangeDto(id, me, "PENDING", sellerId = seller, buyerConfirmedAt = buyerConfirmedAt, sellerConfirmedAt = sellerConfirmedAt)

    // --- Both sides must confirm before the sale closes ---

    @Test fun theBuyerConfirmingAloneWaitsForTheSeller() = runBlocking {
        remote.exchanges += pendingOrder()

        val result = repository.confirm(target, GeoPoint(10.0001, 20.0002))

        assertEquals(ConfirmResult.WaitingForOther, result)
        assertEquals(Completion("pending-1", 10.0001, 20.0002), remote.completions.single())
        assertEquals("PENDING", remote.exchanges.single().status)
        assertFalse(repository.isConfirmed("p1"))
    }

    @Test fun theSellerConfirmingAloneWaitsForTheBuyer() = runBlocking {
        remote.callerIsSeller = true
        remote.exchanges += pendingOrder()

        assertEquals(ConfirmResult.WaitingForOther, repository.confirm(sellerTarget, null))
        assertEquals("PENDING", remote.exchanges.single().status)
        assertTrue(remote.orders.isEmpty())
    }

    @Test fun theSecondConfirmationClosesTheSale() = runBlocking {
        remote.callerIsSeller = true
        remote.exchanges += pendingOrder(buyerConfirmedAt = "2026-11-22T14:58:00Z")

        assertEquals(ConfirmResult.Completed, repository.confirm(sellerTarget, null))
        assertEquals("COMPLETED", remote.exchanges.single().status)
        assertTrue(repository.isConfirmed("p1"))
    }

    @Test fun theSellerNeverOrdersTheirOwnListing() = runBlocking {
        remote.callerIsSeller = true

        assertEquals(ConfirmResult.NoOrder, repository.confirm(sellerTarget, null))
        assertTrue(remote.orders.isEmpty())
        assertTrue(remote.completions.isEmpty())
    }

    @Test fun confirmingAgainWhileWaitingDoesNotPostTwice() = runBlocking {
        remote.exchanges += pendingOrder(buyerConfirmedAt = "2026-11-22T14:58:00Z")

        assertEquals(ConfirmResult.WaitingForOther, repository.confirm(target, null))
        assertTrue(remote.completions.isEmpty())
    }

    @Test fun progressTellsWhetherTheOtherSideConfirmed() = runBlocking {
        remote.exchanges += pendingOrder()
        assertEquals(ExchangeProgress.NOT_CONFIRMED, repository.progress(target))

        repository.confirm(target, null)
        assertEquals(ExchangeProgress.WAITING_FOR_OTHER, repository.progress(target))

        remote.otherSideConfirmsNow()
        assertEquals(ExchangeProgress.COMPLETED, repository.progress(target))
        assertTrue(repository.isConfirmed("p1"))
    }

    @Test fun progressIsUnknownWithoutConnection() = runBlocking {
        remote.failure = IOException("airplane mode")

        assertEquals(ExchangeProgress.UNKNOWN, repository.progress(target))
    }

    // --- Buyer side, as before ---

    @Test fun placesTheOrderFirstWhenADealWasAgreedOnlyInTheChat() = runBlocking {
        remote.otherSideConfirmed = true

        assertEquals(ConfirmResult.Completed, repository.confirm(target, GeoPoint(10.0, 20.0)))
        assertEquals(listOf(material), remote.orders)
        assertEquals("order-1", remote.completions.single().exchangeId)
    }

    @Test fun manualCheckInCompletesWithoutCoordinates() = runBlocking {
        remote.exchanges += pendingOrder()

        repository.confirm(target, null)

        assertEquals(Completion("pending-1", null, null), remote.completions.single())
    }

    @Test fun anExchangeAlreadyCompletedIsNotRecordedAgain() = runBlocking {
        remote.exchanges += ExchangeDto("done-1", me, "COMPLETED")

        assertEquals(ConfirmResult.Completed, repository.confirm(target, null))
        assertTrue(remote.orders.isEmpty())
        assertTrue(remote.completions.isEmpty())
        assertTrue(repository.isConfirmed("p1"))
    }

    @Test fun anotherBuyersOrderIsIgnored() = runBlocking {
        remote.exchanges += ExchangeDto("someone-else", "a0000000-0000-4000-8000-000000000009", "PENDING")

        repository.confirm(target.copy(buyerId = me), null)

        assertEquals(listOf(material), remote.orders)
    }

    @Test fun meetingPointLoadedFromTheApiIsNotNeededToComplete() = runBlocking {
        remote.exchanges += pendingOrder()

        assertEquals(ConfirmResult.WaitingForOther, repository.confirm(target.copy(meetingPointId = "c-uuid"), null))
    }

    @Test fun locallyPublishedListingIsNotSent() = runBlocking {
        assertEquals(ConfirmResult.NotSynced, repository.confirm(target.copy(productId = "local-13"), null))
        assertTrue(remote.orders.isEmpty())
    }

    @Test fun offlineFailureIsNotRememberedSoRetryWorks() = runBlocking {
        remote.otherSideConfirmed = true
        remote.failure = IOException("airplane mode")
        assertEquals(ConfirmResult.Offline, repository.confirm(target, null))

        remote.failure = null
        assertEquals(ConfirmResult.Completed, repository.confirm(target, null))
        assertEquals(1, remote.completions.size)
    }

    @Test fun serverRejectionReportsTheHttpCode() = runBlocking {
        remote.failure = HttpException(Response.error<Any>(409, ResponseBody.create(null, "")))

        assertEquals(ConfirmResult.Rejected(409), repository.confirm(target, null))
    }

    @Test fun completedExchangeIsNotPostedTwiceInTheSameSession() = runBlocking {
        remote.otherSideConfirmed = true
        repository.confirm(target, null)
        repository.confirm(target, null)

        assertEquals(1, remote.completions.size)
    }
}
