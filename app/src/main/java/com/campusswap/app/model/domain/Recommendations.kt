package com.campusswap.app.model.domain

import com.campusswap.app.model.Category
import com.campusswap.app.model.Product
import com.campusswap.app.model.SmartAlert

/** The rule Smart Matching uses to decide that a listing answers one of the user's alerts. */
fun SmartAlert.matches(product: Product): Boolean =
    product.price <= maxPrice &&
        product.condition.ordinal <= minCondition.ordinal &&
        (course == null || product.course?.code == course.code) &&
        (category == Category.ALL || product.category == category) &&
        (
            keyword.isBlank() || product.title.contains(keyword, ignoreCase = true) ||
                product.course?.code?.contains(keyword, ignoreCase = true) == true
            )

/**
 * Home's "Recommended" section. It used to show listings 2–4 of the feed, the same for everyone. Now
 * it ranks the feed by what the user has told the app they want:
 *
 * - +4 if the listing matches one of their active alerts,
 * - +2 if it is for a course they saved in the wishlist or an alert,
 * - +1 if it is in a category they saved or follow.
 *
 * Ties keep the feed order (newest first). Their own listings, queued listings and anything in
 * [exclude] (already saved, in the cart, or shown right above in "New listings") are left out.
 */
object Recommendations {
    fun pick(
        feed: List<Product>,
        myUserId: String,
        wishlist: List<Product>,
        alerts: List<SmartAlert>,
        exclude: Set<String>,
        limit: Int = 3,
    ): List<Product> {
        val active = alerts.filter { it.enabled }
        val categories = (wishlist.map { it.category } + active.map { it.category }).filter { it != Category.ALL }.toSet()
        val courses = (wishlist.mapNotNull { it.course?.code } + active.mapNotNull { it.course?.code }).toSet()

        fun score(product: Product): Int =
            (if (active.any { it.matches(product) }) 4 else 0) +
                (if (product.course?.code?.let { it in courses } == true) 2 else 0) +
                (if (product.category in categories) 1 else 0)

        return feed
            .asSequence()
            .filter { it.seller.id != myUserId && it.pendingSync == null && it.id !in exclude }
            .withIndex()
            .sortedWith(compareByDescending<IndexedValue<Product>> { score(it.value) }.thenBy { it.index })
            .take(limit)
            .map { it.value }
            .toList()
    }
}
