package com.campusswap.app.model.orders

import com.campusswap.app.model.CartLine
import com.campusswap.app.model.Order
import com.campusswap.app.model.Product
import com.campusswap.app.model.SeedIds
import com.campusswap.app.model.materials.toProduct
import com.campusswap.app.model.remote.ExchangeDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import com.campusswap.app.model.local.ResponseCache
import com.campusswap.app.model.local.load
import com.campusswap.app.model.local.save

sealed interface PlaceOrderResult {
    /**
     * Some listings were ordered. [unavailable] are gone for good (someone else reserved them, they are
     * yours, or they were deleted) and leave the cart. [notSent] were not ordered because the connection
     * or the server failed: they stay in the cart so the buyer can try again.
     */
    data class Placed(val order: Order, val unavailable: List<Product>, val notSent: List<Product> = emptyList()) : PlaceOrderResult
    /** Nothing was ordered; same meaning for [unavailable] and [notSent] as in [Placed]. */
    data class Rejected(val unavailable: List<Product>, val notSent: List<Product> = emptyList()) : PlaceOrderResult
    /** No connection from the first request: nothing was ordered and the cart stays as it was. */
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

    /**
     * Una orden del backend por producto (POST /exchanges/orders): cada material queda RESERVED.
     *
     * Un error del servidor (5xx, 429) se reintenta; un 409 o una respuesta que no llegó pueden ser un
     * pedido que sí quedó hecho (p. ej. el servidor reservó el material pero la respuesta se perdió y al
     * reintentar contesta 409). Esos casos se confirman al final con GET /exchanges usando [buyerId],
     * para no decirle al comprador que su propio pedido "ya no está disponible".
     */
    suspend fun place(
        lines: List<CartLine>,
        meetOnCampus: Boolean,
        buyerId: String? = null,
        retryDelayMs: Long = RETRY_DELAY_MS,
    ): PlaceOrderResult {
        val placed = mutableMapOf<String, ExchangeDto>()
        val unavailable = mutableListOf<Product>()
        // Pedidos cuyo resultado no se sabe: 409 (¿lo reservé yo?) o sin respuesta
        val taken = mutableListOf<Product>()
        val unsure = mutableListOf<Product>()
        val notSent = mutableListOf<Product>()
        var stopped = false
        var noConnectionFirst = false
        for (line in lines) {
            val product = line.product
            // Una publicación pendiente del Outbox todavía no existe en el servidor
            if (!SeedIds.isBackendId(product.id)) {
                unavailable += product
                continue
            }
            // Sin red o sesión vencida: lo que falta ni se intenta y se queda en el carrito
            if (stopped) {
                notSent += product
                continue
            }
            when (val attempt = placeOne(product.id, retryDelayMs)) {
                is Attempt.Done -> placed[product.id] = attempt.exchange
                Attempt.Gone -> unavailable += product
                Attempt.Taken -> taken += product
                Attempt.ServerError -> unsure += product
                Attempt.NoConnection -> {
                    if (placed.isEmpty() && taken.isEmpty() && unsure.isEmpty()) noConnectionFirst = true
                    unsure += product
                    stopped = true
                }
                Attempt.SessionExpired -> {
                    notSent += product
                    stopped = true
                }
            }
        }

        if (taken.isNotEmpty() || unsure.isNotEmpty()) {
            val mine = myOpenPurchases(buyerId)
            taken.forEach { product ->
                val exchange = mine?.get(product.id)
                when {
                    exchange != null -> placed[product.id] = exchange
                    // Confirmado: lo reservó otra persona
                    mine != null -> unavailable += product
                    else -> notSent += product
                }
            }
            unsure.forEach { product ->
                val exchange = mine?.get(product.id)
                if (exchange != null) placed[product.id] = exchange else notSent += product
            }
        }

        val orderedLines = lines.filter { it.product.id in placed }
        if (orderedLines.isEmpty()) {
            if (noConnectionFirst && unavailable.isEmpty()) return PlaceOrderResult.Offline
            return PlaceOrderResult.Rejected(unavailable, inCartOrder(lines, notSent))
        }
        val order = Order(
            number = placed.getValue(orderedLines.first().product.id).orderNumber ?: 0,
            // Cada material es una sola unidad en un marketplace de segunda mano
            lines = orderedLines.map { CartLine(it.product, 1) },
            meetOnCampus = meetOnCampus,
        )
        return PlaceOrderResult.Placed(order, unavailable, inCartOrder(lines, notSent))
    }

    private sealed interface Attempt {
        data class Done(val exchange: ExchangeDto) : Attempt
        /** 400 (tu propia publicación) o 404 (ya no existe): no se puede pedir */
        data object Gone : Attempt
        /** 409: ya está reservado, quizá por ti mismo en un intento anterior */
        data object Taken : Attempt
        data object ServerError : Attempt
        data object NoConnection : Attempt
        data object SessionExpired : Attempt
    }

    private suspend fun placeOne(materialId: String, retryDelayMs: Long): Attempt {
        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                return Attempt.Done(remote.place(materialId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                return Attempt.NoConnection
            } catch (e: HttpException) {
                when (e.code()) {
                    401 -> return Attempt.SessionExpired
                    409 -> return Attempt.Taken
                    in 400..499 -> if (e.code() != 408 && e.code() != 429) return Attempt.Gone
                }
                // 5xx, 408 o 429: el servidor está ocupado o falló; se espera un poco y se reintenta
                if (attempt < MAX_ATTEMPTS) delay(retryDelayMs * attempt)
            }
        }
        return Attempt.ServerError
    }

    /** Material id -> mi intercambio abierto; null si no se pudo preguntar. */
    private suspend fun myOpenPurchases(buyerId: String?): Map<String, ExchangeDto>? {
        if (buyerId == null) return null
        val exchanges = try {
            remote.mine()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        }
        return exchanges
            .filter { it.buyerId == buyerId && it.status != "CANCELLED" && it.materialId != null }
            .associateBy { it.materialId!! }
    }

    private fun inCartOrder(lines: List<CartLine>, products: List<Product>): List<Product> {
        val ids = products.map { it.id }.toSet()
        return lines.map { it.product }.filter { it.id in ids }
    }

    /**
     * Cache then network: primero emite la copia guardada (al instante, sin esperar la red)
     * y luego la del servidor, que la reemplaza y queda guardada. */
    fun stream(userId: String, showSaved: Boolean = true): Flow<MyExchanges> = flow {
        val saved = cache.load<List<ExchangeDto>>(userId, KEY)
        // 1. Cache
        if (showSaved && saved != null) {
            emit(toMyExchanges(saved.value, userId, isLive = false, savedAtMillis = saved.savedAtMillis))
        }
        // 2. Network
        val fromServer = try {
            remote.mine()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (fromServer != null) {
            cache.save(userId, KEY, fromServer)
            emit(toMyExchanges(fromServer, userId, isLive = true, savedAtMillis = null))
        } else if (!showSaved && saved != null) {
            // Sin red y la copia no se había mostrado: se muestra ahora, marcada como offline
            emit(toMyExchanges(saved.value, userId, isLive = false, savedAtMillis = saved.savedAtMillis))
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
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 700L
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