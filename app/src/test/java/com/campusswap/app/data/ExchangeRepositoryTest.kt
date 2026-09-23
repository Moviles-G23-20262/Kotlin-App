package com.campusswap.app.data

import com.campusswap.app.domain.ExchangeTarget
import com.campusswap.app.domain.GeoPoint
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Locale

class ExchangeRepositoryTest {
    private val remote = FakeExchangeRemoteDataSource()
    private val repository = ExchangeRepository(remote)
    private val target = ExchangeTarget("p1", "me", "s1", 320000.0, "mp1", GeoPoint(10.0, 20.0))
    private val defaultLocale = Locale.getDefault()

    @After fun restoreLocale() = Locale.setDefault(defaultLocale)

    @Test fun mapsLocalIdsToSeedUuidsAndSendsCoordinates() = runBlocking {
        val result = repository.confirm(target, GeoPoint(10.0001, 20.0002))

        assertEquals(ConfirmResult.Success, result)
        val request = remote.requests.single()
        assertEquals("b0000000-0000-4000-8000-000000000001", request.materialId)
        assertEquals("a0000000-0000-4000-8000-000000000000", request.buyerId)
        assertEquals("a0000000-0000-4000-8000-000000000001", request.sellerId)
        assertEquals("c0000000-0000-4000-8000-000000000001", request.meetingPointId)
        assertEquals(10.0001, request.lat!!, 0.0)
        assertEquals(20.0002, request.lng!!, 0.0)
    }

    @Test fun priceUsesADotEvenOnASpanishLocale() = runBlocking {
        Locale.setDefault(Locale("es", "CO"))

        repository.confirm(target, null)

        assertEquals("320000.00", remote.requests.single().price)
    }

    @Test fun manualCheckInSendsNoCoordinates() = runBlocking {
        repository.confirm(target, null)

        val request = remote.requests.single()
        assertNull(request.lat)
        assertNull(request.lng)
        assertEquals("c0000000-0000-4000-8000-000000000001", request.meetingPointId)
    }

    @Test fun locallyPublishedListingIsNotSent() = runBlocking {
        val result = repository.confirm(target.copy(productId = "local-13"), null)

        assertEquals(ConfirmResult.NotSynced, result)
        assertTrue(remote.requests.isEmpty())
    }

    @Test fun offlineFailureIsNotRememberedSoRetryWorks() = runBlocking {
        remote.failure = IOException("airplane mode")
        assertEquals(ConfirmResult.Offline, repository.confirm(target, null))

        remote.failure = null
        assertEquals(ConfirmResult.Success, repository.confirm(target, null))
        assertEquals(1, remote.requests.size)
    }

    @Test fun serverRejectionReportsTheHttpCode() = runBlocking {
        remote.failure = HttpException(Response.error<Any>(400, ResponseBody.create(null, "")))

        assertEquals(ConfirmResult.Rejected(400), repository.confirm(target, null))
    }

    @Test fun confirmedExchangeIsNotPostedTwice() = runBlocking {
        repository.confirm(target, null)
        repository.confirm(target, null)

        assertTrue(repository.isConfirmed("p1"))
        assertEquals(1, remote.requests.size)
    }
}
