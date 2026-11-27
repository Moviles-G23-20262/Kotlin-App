package com.campusswap.app.model.orders

import com.campusswap.app.model.CartLine
import com.campusswap.app.model.SampleData
import com.campusswap.app.model.remote.ExchangeDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import com.campusswap.app.model.local.InMemoryResponseCache
import com.campusswap.app.model.local.save
import com.campusswap.app.model.materials.MaterialDto
import kotlinx.coroutines.flow.toList

class OrderRepositoryTest {
    private val ids = listOf(
        "c0000000-0000-4000-8000-000000000001",
        "c0000000-0000-4000-8000-000000000002",
        "c0000000-0000-4000-8000-000000000003",
    )
    private val cart = ids.mapIndexed { i, id -> CartLine(SampleData.products[i].copy(id = id), 1) }

    private class ConnectionDropsAfter(private val answered: Int) : OrdersRemoteDataSource {
        var calls = 0
        override suspend fun mine(): List<ExchangeDto> = emptyList()
        override suspend fun place(materialId: String): ExchangeDto {
            calls++
            if (calls > answered) throw IOException("connection lost")
            return ExchangeDto(id = "e-$materialId", buyerId = "me", status = "PENDING", orderNumber = 1002, materialId = materialId)
        }
    }

    @Test fun keepsTheItemsThatWereNotSentWhenTheConnectionDropsMidOrder() = runBlocking {
        val remote = ConnectionDropsAfter(answered = 1)

        val result = OrderRepository(remote).place(cart, meetOnCampus = true) as PlaceOrderResult.Placed

        assertEquals(listOf(ids[0]), result.order.products.map { it.id })
        assertEquals(listOf(ids[1], ids[2]), result.notSent.map { it.id })
        assertTrue(result.unavailable.isEmpty())
        assertEquals(2, remote.calls)
    }

    @Test fun placesNothingWhenThereIsNoConnectionFromTheStart() = runBlocking {
        val result = OrderRepository(ConnectionDropsAfter(answered = 0)).place(cart, meetOnCampus = true)

        assertEquals(PlaceOrderResult.Offline, result)
    }

    @Test fun placesEverythingWhenTheConnectionHolds() = runBlocking {
        val result = OrderRepository(ConnectionDropsAfter(answered = 3)).place(cart, meetOnCampus = true) as PlaceOrderResult.Placed

        assertEquals(ids, result.order.products.map { it.id })
        assertTrue(result.notSent.isEmpty())
    }
    // --- A busy server, a lost answer or someone else first: what the buyer is told must be true ---

    private val buyer = "b0000000-0000-4000-8000-000000000000"

    /** Answers each POST /exchanges/orders from a script per material; GET /exchanges lists what was reserved. */
    private class ScriptedServer(
        private val buyer: String,
        private val script: Map<String, MutableList<Any>> = emptyMap(),
        private val takenByOthers: Set<String> = emptySet(),
        private val mineFails: Boolean = false,
    ) : OrdersRemoteDataSource {
        val reserved = linkedMapOf<String, ExchangeDto>()
        val calls = mutableListOf<String>()

        override suspend fun mine(): List<ExchangeDto> {
            if (mineFails) throw IOException("no connection")
            return reserved.values.toList()
        }

        override suspend fun place(materialId: String): ExchangeDto {
            calls += materialId
            if (materialId in takenByOthers || materialId in reserved) throw http(409)
            when (val next = script[materialId]?.removeFirstOrNull()) {
                null, "ok" -> Unit
                // The server reserves the listing but the answer never reaches the phone
                "lost" -> { reserve(materialId); throw SocketTimeoutException("timeout") }
                is Throwable -> throw next
            }
            return reserve(materialId)
        }

        private fun reserve(materialId: String) = ExchangeDto(
            id = "e-$materialId", buyerId = buyer, status = "PENDING",
            orderNumber = 2000 + reserved.size, materialId = materialId,
        ).also { reserved[materialId] = it }
    }

    private fun bigCart(n: Int) = (1..n).map { i ->
        CartLine(SampleData.products[i % SampleData.products.size].copy(id = "d0000000-0000-4000-8000-%012d".format(i), title = "Item $i"), 1)
    }

    @Test fun retriesWhenTheServerFailsForAMomentInsteadOfCallingTheItemUnavailable() = runBlocking {
        val server = ScriptedServer(buyer, mapOf(ids[1] to mutableListOf<Any>(http(500), http(503))))

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(ids, result.order.products.map { it.id })
        assertTrue(result.unavailable.isEmpty())
        assertTrue(result.notSent.isEmpty())
    }

    @Test fun keepsTheItemInTheCartWhenTheServerKeepsFailing() = runBlocking {
        val server = ScriptedServer(buyer, mapOf(ids[0] to mutableListOf<Any>(http(500), http(500), http(500))))

        val result = OrderRepository(server).place(cart.take(1), meetOnCampus = true, buyerId = buyer, retryDelayMs = 0)

        assertEquals(PlaceOrderResult.Rejected(unavailable = emptyList(), notSent = listOf(cart[0].product)), result)
        assertEquals(3, server.calls.size)
    }

    @Test fun anOrderWhoseAnswerWasLostCountsAsPlacedInsteadOfUnavailable() = runBlocking {
        val server = ScriptedServer(buyer, mapOf(ids[1] to mutableListOf<Any>("lost")))

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        // The second listing was reserved even though the phone got no answer
        assertTrue(ids[1] in result.order.products.map { it.id })
        assertTrue(result.unavailable.isEmpty())
        // The third one wasn't sent after the connection failed: it stays in the cart
        assertEquals(listOf(ids[2]), result.notSent.map { it.id })
    }

    @Test fun retryingAfterALostAnswerDoesNotCallYourOwnReservationUnavailable() = runBlocking {
        val server = ScriptedServer(buyer)
        server.place(ids[0]) // first try went through, the phone never heard back

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(ids, result.order.products.map { it.id })
        assertTrue(result.unavailable.isEmpty())
    }

    @Test fun aListingSomeoneElseReservedLeavesTheCart() = runBlocking {
        val server = ScriptedServer(buyer, takenByOthers = setOf(ids[2]))

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(listOf(ids[0], ids[1]), result.order.products.map { it.id })
        assertEquals(listOf(ids[2]), result.unavailable.map { it.id })
        assertTrue(result.notSent.isEmpty())
    }

    @Test fun whenTheBuyerCannotBeCheckedATakenListingStaysInTheCart() = runBlocking {
        val server = ScriptedServer(buyer, takenByOthers = setOf(ids[0]), mineFails = true)

        val result = OrderRepository(server).place(cart.take(1), meetOnCampus = true, buyerId = buyer, retryDelayMs = 0)

        assertEquals(PlaceOrderResult.Rejected(unavailable = emptyList(), notSent = listOf(cart[0].product)), result)
    }

    @Test fun anExpiredSessionStopsTheOrderAndKeepsTheRestInTheCart() = runBlocking {
        val server = ScriptedServer(buyer, mapOf(ids[1] to mutableListOf<Any>(http(401))))

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(listOf(ids[0]), result.order.products.map { it.id })
        assertEquals(listOf(ids[1], ids[2]), result.notSent.map { it.id })
        assertEquals(listOf(ids[0], ids[1]), server.calls)
    }

    @Test fun aBigCartWithAFlakyServerIsOrderedCompletely() = runBlocking {
        val lines = bigCart(15)
        // Every third listing gets one 503 before going through
        val script = lines.withIndex().filter { it.index % 3 == 0 }.associate { it.value.product.id to mutableListOf<Any>(http(503)) }
        val server = ScriptedServer(buyer, script)

        val result = OrderRepository(server).place(lines, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(lines.map { it.product.id }, result.order.products.map { it.id })
        assertEquals(15, server.reserved.size)
        assertTrue(result.unavailable.isEmpty() && result.notSent.isEmpty())
    }

    @Test fun ownListingsAndDeletedListingsAreUnavailableWithoutRetrying() = runBlocking {
        val server = ScriptedServer(buyer, mapOf(ids[0] to mutableListOf<Any>(http(400)), ids[1] to mutableListOf<Any>(http(404))))

        val result = OrderRepository(server).place(cart, meetOnCampus = true, buyerId = buyer, retryDelayMs = 0) as PlaceOrderResult.Placed

        assertEquals(listOf(ids[2]), result.order.products.map { it.id })
        assertEquals(listOf(ids[0], ids[1]), result.unavailable.map { it.id })
        assertEquals(listOf(ids[0], ids[1], ids[2]), server.calls)
    }

        private class ExchangesServer(private val answer: List<ExchangeDto>?) : OrdersRemoteDataSource {
        override suspend fun mine(): List<ExchangeDto> = answer ?: throw IOException("no connection")
        override suspend fun place(materialId: String): ExchangeDto = throw IOException("no connection")
    }

    private val me = "a0000000-0000-4000-8000-000000000000"

    private fun purchase(title: String) = ExchangeDto(
        id = "e-$title", buyerId = me, status = "PENDING", orderNumber = 1001, materialId = "m-$title",
        material = MaterialDto("m-$title", title, "", null, "1000", null, "RESERVED", "SUPPLIES", "s1"),
    )

    private fun savedCopyOf(vararg exchanges: ExchangeDto) =
        InMemoryResponseCache().also { runBlocking { it.save(me, "my-exchanges", exchanges.toList()) } }

    @Test fun showsTheSavedCopyFirstAndThenTheServerAnswer() = runBlocking {
        val repository = OrderRepository(ExchangesServer(listOf(purchase("New"))), savedCopyOf(purchase("Old")))

        val shown = repository.stream(me).toList()

        assertEquals(listOf(false, true), shown.map { it.isLive })
        assertEquals(listOf("Old", "New"), shown.map { it.purchases.single().products.single().title })
    }

    @Test fun keepsTheSavedCopyMarkedOfflineWhenTheServerDoesNotAnswer() = runBlocking {
        val shown = OrderRepository(ExchangesServer(null), savedCopyOf(purchase("Old"))).stream(me).toList()

        assertEquals(1, shown.size)
        assertEquals(false, shown.single().isLive)
    }

    @Test fun doesNotGoBackToTheSavedCopyWhenTheScreenAlreadyHasData() = runBlocking {
        val repository = OrderRepository(ExchangesServer(listOf(purchase("New"))), savedCopyOf(purchase("Old")))

        val shown = repository.stream(me, showSaved = false).toList()

        assertEquals(listOf("New"), shown.map { it.purchases.single().products.single().title })
    }

}

private fun http(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "")))
