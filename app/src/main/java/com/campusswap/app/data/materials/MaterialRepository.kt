package com.campusswap.app.data.materials

import com.campusswap.app.data.Product
import kotlinx.coroutines.CancellationException

data class MaterialFeed(val products: List<Product>, val isLive: Boolean)

class MaterialRepository(private val remote: MaterialRemoteDataSource) {
    private var cached: List<Product> = emptyList()

    suspend fun load(): MaterialFeed = try {
        val products = remote.getMaterials()
            .filter { it.status == "AVAILABLE" }
            .map { it.toProduct() }
        cached = products
        MaterialFeed(products, isLive = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        MaterialFeed(cached, isLive = false)
    }
}