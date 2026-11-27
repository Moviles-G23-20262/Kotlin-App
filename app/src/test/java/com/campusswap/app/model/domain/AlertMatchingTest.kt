package com.campusswap.app.model.domain

import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.Product
import com.campusswap.app.model.Seller
import com.campusswap.app.model.SmartAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertMatchingTest {
    private val me = Seller("me", "Me", true, null, 0, "m***@uniandes.edu.co")
    private val other = Seller("other", "Other", true, 4.5, 3, "o***@uniandes.edu.co")
    private val now = 10_000_000L

    private fun product(id: String, title: String, seller: Seller = other, minutesAgo: Long = 0) = Product(
        id = id, title = title, description = "", price = 20_000.0, category = Category.TEXTBOOKS,
        course = null, condition = Condition.GOOD, rating = null, reviewCount = 0,
        seller = seller, imageSeed = 0, createdAtMillis = now - minutesAgo * 60_000,
    )

    private fun alert(id: String, keyword: String, enabled: Boolean = true) = SmartAlert(
        id = id, keyword = keyword, course = null, category = Category.ALL,
        maxPrice = 100_000.0, minCondition = Condition.FAIR, enabled = enabled,
    )

    @Test
    fun `a listing matched by two alerts appears once, under the first one`() {
        val found = findAlertMatches(
            listOf(alert("a", "calculo"), alert("b", "stewart")),
            listOf(product("1", "Calculo Stewart")),
            "me", emptySet(), now,
        )
        assertEquals(listOf("1"), found.map { it.product.id })
        assertEquals("a", found.single().alert.id)
    }

    @Test
    fun `own listings and disabled alerts are left out`() {
        val found = findAlertMatches(
            listOf(alert("a", "calculo"), alert("b", "fisica", enabled = false)),
            listOf(product("1", "Calculo", seller = me), product("2", "Fisica")),
            "me", emptySet(), now,
        )
        assertTrue(found.isEmpty())
    }

    @Test
    fun `newest first and reserved listings are marked`() {
        val found = findAlertMatches(
            listOf(alert("a", "calculo")),
            listOf(product("old", "Calculo I", minutesAgo = 90), product("new", "Calculo II", minutesAgo = 5)),
            "me", setOf("old"), now,
        )
        assertEquals(listOf("new", "old"), found.map { it.product.id })
        assertTrue(found.last().reserved)
    }
}