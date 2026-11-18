package com.campusswap.app.model.sellers

import com.campusswap.app.model.Seller
import com.campusswap.app.model.materials.MaterialDto
import com.campusswap.app.model.materials.MaterialSellerDto
import com.campusswap.app.model.ratings.RatingDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SellerProfileTest {
    private val sellerDto = MaterialSellerDto(id = "s1", email = null, fullName = "Ana Torres", rating = 4.0, major = "Systems Engineering")

    private fun listing(id: String, status: String) = MaterialDto(
        id = id, title = "Item $id", description = "", courseCode = null, price = "20000", condition = "GOOD",
        status = status, category = "TEXTBOOKS", sellerId = "s1", seller = sellerDto,
    )

    private fun rating(id: String, stars: Int, tags: List<String> = emptyList(), review: String? = null) = RatingDto(
        id = id, exchangeId = "e$id", raterId = "b$id", ratedId = "s1", stars = stars, tags = tags, review = review,
        createdAt = "2026-11-10T15:00:00Z", rater = null,
    )

    @Test
    fun `average, star counts and most repeated tags come from the ratings`() {
        val profile = buildSellerProfile(
            "s1",
            listOf(
                rating("1", 5, listOf("Punctual", "Friendly")),
                rating("2", 4, listOf("Punctual")),
                rating("3", 3, listOf("Punctual", "Fair price")),
            ),
            listOf(listing("a", "AVAILABLE")),
            fallback = null,
        )
        assertEquals(4.0, profile.averageStars!!, 0.001)
        assertEquals(listOf(0, 0, 1, 1, 1), profile.starCounts)
        assertEquals(TagCount("Punctual", 3), profile.topTags.first())
        assertEquals(3, profile.reviewCount)
    }

    @Test
    fun `listings are split into available and sold`() {
        val profile = buildSellerProfile(
            "s1",
            emptyList(),
            listOf(listing("a", "AVAILABLE"), listing("b", "SOLD"), listing("c", "SOLD"), listing("d", "RESERVED")),
            fallback = null,
        )
        assertEquals(listOf("a"), profile.availableListings.map { it.id })
        assertEquals(2, profile.soldCount)
        assertEquals("Ana Torres", profile.name)
    }

    @Test
    fun `without ratings or listings the profile uses what the catalogue already knew`() {
        val fallback = Seller("s1", "Ana Torres", true, null, 0, "a***@uniandes.edu.co", major = "Design")
        val profile = buildSellerProfile("s1", emptyList(), emptyList(), fallback)
        assertEquals("Ana Torres", profile.name)
        assertEquals("Design", profile.major)
        assertNull(profile.averageStars)
        assertEquals(listOf(0, 0, 0, 0, 0), profile.starCounts)
    }

    @Test
    fun `blank written reviews are treated as no text`() {
        val profile = buildSellerProfile("s1", listOf(rating("1", 5, review = "   ")), emptyList(), fallback = null)
        assertNull(profile.reviews.single().text)
    }
}