package com.campusswap.app.model.domain

import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.Course
import com.campusswap.app.model.Product
import com.campusswap.app.model.Seller
import com.campusswap.app.model.SmartAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsTest {
    private val me = Seller("me", "Me", true, null, 0, "m***@uniandes.edu.co")
    private val other = Seller("other", "Other", true, 4.5, 3, "o***@uniandes.edu.co")

    private fun product(
        id: String,
        category: Category = Category.SUPPLIES,
        title: String = "Item $id",
        course: String? = null,
        seller: Seller = other,
        price: Double = 20_000.0,
    ) = Product(
        id = id, title = title, description = "", price = price, category = category,
        course = course?.let { Course(it, it) }, condition = Condition.GOOD, rating = null, reviewCount = 0,
        seller = seller, imageSeed = 0,
    )

    private fun alert(category: Category = Category.ALL, keyword: String = "", course: String? = null) = SmartAlert(
        id = "a", keyword = keyword, course = course?.let { Course(it, it) }, category = category,
        maxPrice = 100_000.0, minCondition = Condition.FAIR,
    )

    @Test
    fun `without any interest it keeps the newest listings`() {
        val feed = listOf(product("1"), product("2"), product("3"), product("4"))

        val picked = Recommendations.pick(feed, "me", wishlist = emptyList(), alerts = emptyList(), exclude = setOf("1"))

        assertEquals(listOf("2", "3", "4"), picked.map { it.id })
    }

    @Test
    fun `a listing that matches an alert comes first, then the saved course, then the saved category`() {
        val feed = listOf(
            product("plain"),
            product("category", category = Category.TEXTBOOKS),
            product("course", course = "MATE1203"),
            product("alert", title = "Casio fx-991", category = Category.CALCULATORS),
        )
        val wishlist = listOf(product("saved", category = Category.TEXTBOOKS, course = "MATE1203"))

        val picked = Recommendations.pick(feed, "me", wishlist, listOf(alert(keyword = "casio")), exclude = emptySet())

        assertEquals(listOf("alert", "course", "category"), picked.map { it.id })
    }

    @Test
    fun `my own listings, excluded ones and disabled alerts are left out`() {
        val feed = listOf(product("mine", seller = me), product("inCart"), product("x", title = "Casio"), product("y"))
        val disabled = alert(keyword = "casio").copy(enabled = false)

        val picked = Recommendations.pick(feed, "me", emptyList(), listOf(disabled), exclude = setOf("inCart"))

        assertEquals(listOf("x", "y"), picked.map { it.id })
    }

    @Test
    fun `the alert rule checks price, category, course and keyword`() {
        assertTrue(alert(category = Category.CALCULATORS).matches(product("1", category = Category.CALCULATORS)))
        assertFalse(alert(category = Category.CALCULATORS).matches(product("1", category = Category.TEXTBOOKS)))
        assertFalse(alert().matches(product("1", price = 200_000.0)))
        assertTrue(alert(keyword = "mate1203").matches(product("1", course = "MATE1203")))
        assertFalse(alert(course = "FISI1018").matches(product("1", course = "MATE1203")))
    }
}
