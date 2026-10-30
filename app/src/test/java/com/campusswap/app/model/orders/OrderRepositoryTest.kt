package com.campusswap.app.model.orders

import com.campusswap.app.model.CartLine
import com.campusswap.app.model.SampleData
import com.campusswap.app.model.remote.ExchangeDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
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