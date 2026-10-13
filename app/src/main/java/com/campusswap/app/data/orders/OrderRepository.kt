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
)

/** Lo que devuelve GET /exchanges, segun el intercambio. */
data class MyExchanges(val purchases: List<Order>, val sales: List<Sale>)

class OrderRepository(private val remote: OrdersRemoteDataSource) {

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

    /** Tus compras y tus ventas, o null si no se pudo cargar. */
    suspend fun load(userId: String): MyExchanges? = try {
        val all = remote.mine().filter { it.status != "CANCELLED" }
        MyExchanges(
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
                        )
                    }
                },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
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