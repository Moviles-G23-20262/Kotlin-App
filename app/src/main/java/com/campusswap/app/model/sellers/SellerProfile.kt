package com.campusswap.app.model.sellers

import com.campusswap.app.model.Product
import com.campusswap.app.model.Seller
import com.campusswap.app.model.materials.MaterialDto
import com.campusswap.app.model.materials.toProduct
import com.campusswap.app.model.ratings.RatingDto
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.Instant

/** One review a seller received, ready for the screen. */
data class SellerReview(
    val id: String,
    val stars: Int,
    val tags: List<String>,
    val text: String?,
    val raterName: String?,
    val createdAtMillis: Long?,
)

data class TagCount(val tag: String, val count: Int)

/** Everything the Seller profile view shows. */
data class SellerProfile(
    val sellerId: String,
    val name: String,
    val major: String?,
    val faculty: String?,
    val averageStars: Double?,
    val reviewCount: Int,
    /** Index 0 = reviews with 1 star ... index 4 = reviews with 5 stars. */
    val starCounts: List<Int>,
    val topTags: List<TagCount>,
    val reviews: List<SellerReview>,
    val availableListings: List<Product>,
    val soldCount: Int,
)

/** The two public endpoints the profile needs, called at the same time by the repository. */
interface SellerProfileApi {
    @GET("ratings")
    suspend fun ratingsFor(@Query("userId") userId: String): List<RatingDto>

    @GET("materials")
    suspend fun listingsBy(@Query("sellerId") sellerId: String): List<MaterialDto>
}

/**
 * Turns the raw server answers into what the screen shows. Pure function: it runs on
 * Dispatchers.Default so grouping and counting never block the main thread.
 */
fun buildSellerProfile(
    sellerId: String,
    ratings: List<RatingDto>,
    listings: List<MaterialDto>,
    fallback: Seller?,
): SellerProfile {
    val seller = listings.firstNotNullOfOrNull { it.seller }
    val starCounts = IntArray(5)
    ratings.forEach { starCounts[it.stars.coerceIn(1, 5) - 1]++ }
    val average = if (ratings.isEmpty()) {
        seller?.rating?.takeIf { it > 0 } ?: fallback?.rating
    } else {
        ratings.sumOf { it.stars }.toDouble() / ratings.size
    }
    val topTags = ratings.asSequence()
        .flatMap { it.tags.orEmpty().asSequence() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(MAX_TAGS)
        .map { TagCount(it.key, it.value) }
    val products = listings.map { it.toProduct() }

    return SellerProfile(
        sellerId = sellerId,
        name = seller?.fullName ?: fallback?.name ?: "CampusSwap student",
        major = seller?.major ?: fallback?.major,
        faculty = seller?.faculty,
        averageStars = average,
        reviewCount = ratings.size,
        starCounts = starCounts.toList(),
        topTags = topTags,
        reviews = ratings.map { rating ->
            SellerReview(
                id = rating.id,
                stars = rating.stars,
                tags = rating.tags.orEmpty(),
                text = rating.review?.takeIf { it.isNotBlank() },
                raterName = rating.rater?.fullName,
                createdAtMillis = rating.createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            )
        },
        availableListings = products.filter { it.status == "AVAILABLE" },
        soldCount = products.count { it.status == "SOLD" },
    )
}

private const val MAX_TAGS = 3