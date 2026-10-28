package com.campusswap.app.data.orders

import com.campusswap.app.data.CartLine
import com.campusswap.app.data.Order
import com.campusswap.app.data.Product
import com.campusswap.app.data.SeedIds
import com.campusswap.app.data.materials.toProduct
import com.campusswap.app.data.remote.ExchangeDto
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save

sealed interface PlaceOrderResult {
    /** [unavailable]: productos del carrito que no se pudieron pedir */
    data class Placed(val order: Order, val unavailable: List<Product>) : PlaceOrderResult
    data class Rejected(val unavailable: List<Product>) : PlaceOrderResult
    data object Offline : PlaceOrderResult
}

/** Una venta tuya: alguien compro (PENDING = reservado, COMPLETED = vendido). */
data class Sale(
    val product: Product,
    val buyerName: String,
    val status: String,
    val completedAtMillis: Long?,
    val buyerId: String? = null,
)

/** Lo que devuelve GET /exchanges, segun el intercambio. */
data class MyExchanges(
    val purchases: List<Order>,
    val sales: List<Sale>,
    /** false = copia guardada en el tel porque no hubo red */
    val isLive: Boolean = true,
    val savedAtMillis: Long? = null,
)

class OrderRepository(
    private val remote: OrdersRemoteDataSource,
    private val cache: ResponseCache = ResponseCache.None,
) {

    /** Una orden del backend por producto (POST /exchanges/orders): cada material queda RESERVED. */
    suspend fun place(lines: List<CartLine>, meetOnCampus: Boolean): PlaceOrderResult {
        val placed = mutableListOf<Pair<Product, ExchangeDto>>()
        val unavailable = mutableListOf<Product>()
        for (line in lines) {
            val product = line.product
            // Una publicación pendiente del Outbox todavía no existe en el servidor
            if (!SeedIds.isBackendId(product.id)) {
                unavailable += product
                continue
            }
            try {
                placed += product to remote.place(product.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // Sin red antes de pedir nada: no se hizo ningún pedido, el carrito queda igual
                if (placed.isEmpty()) return PlaceOrderResult.Offline
                unavailable += product
            } catch (e: HttpException) {
                // 409: ya lo reservó otra persona · 400: es tu propia publicación · 404: ya no existe
                unavailable += product
            }
        }
        if (placed.isEmpty()) return PlaceOrderResult.Rejected(unavailable)
        val order = Order(
            number = placed.first().second.orderNumber ?: 0,
            // Cada material es una sola unidad en un marketplace de segunda mano
            lines = placed.map { (product, _) -> CartLine(product, 1) },
            meetOnCampus = meetOnCampus,
        )
        return PlaceOrderResult.Placed(order, unavailable)
    }

    /** Tus compras y ventas. Sin red: la última copia guardada; null solo si nunca se guardó. */
    suspend fun load(userId: String): MyExchanges? = try {
        val fromServer = remote.mine()
        cache.save(userId, KEY, fromServer)
        toMyExchanges(fromServer, userId, isLive = true, savedAtMillis = null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        cache.load<List<ExchangeDto>>(userId, KEY)?.let { saved ->
            toMyExchanges(saved.value, userId, isLive = false, savedAtMillis = saved.savedAtMillis)
        }
    }

    private fun toMyExchanges(exchanges: List<ExchangeDto>, userId: String, isLive: Boolean, savedAtMillis: Long?): MyExchanges {
        val all = exchanges.filter { it.status != "CANCELLED" }
        return MyExchanges(
            purchases = all
                .filter { it.buyerId == userId }
                .distinctBy { it.materialId }
                .mapNotNull { exchange ->
                    exchange.toProduct()?.let { product -> Order(exchange.orderNumber ?: 0, listOf(CartLine(product, 1))) }
                },
            sales = all
                .filter { it.sellerId == userId }
                .distinctBy { it.materialId }
                .mapNotNull { exchange ->
                    exchange.toProduct()?.let { product ->
                        Sale(
                            product = product,
                            buyerName = exchange.buyer?.fullName ?: "A student",
                            status = exchange.status,
                            completedAtMillis = exchange.completedAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
                            buyerId = exchange.buyerId,
                        )
                    }
                },
            isLive = isLive,
            savedAtMillis = savedAtMillis,
        )
    }

    private companion object {
        const val KEY = "my-exchanges"
    }

    /** El material del intercambio no trae su vendedor; el intercambio sí (exchange.seller). */
    private fun ExchangeDto.toProduct(): Product? {
        val product = material?.toProduct() ?: return null
        val summary = seller ?: return product
        return product.copy(
            seller = product.seller.copy(
                name = summary.fullName,
                isVerified = true,
                rating = summary.rating?.takeIf { it > 0 },
                major = summary.major,
            ),
        )
    }
}