package com.campusswap.app.model.domain

import com.campusswap.app.model.AlertMatch
import com.campusswap.app.model.Product
import com.campusswap.app.model.SmartAlert

fun findAlertMatches(
    alerts: List<SmartAlert>,
    products: List<Product>,
    myUserId: String,
    reserved: Set<String>,
    nowMillis: Long,
): List<AlertMatch> {
    val active = alerts.filter { it.enabled }
    if (active.isEmpty() || products.isEmpty()) return emptyList()
    val seen = HashSet<String>()
    val found = ArrayList<AlertMatch>()
    for (alert in active) {
        for (product in products) {
            if (product.seller.id == myUserId || product.id in seen || !alert.matches(product)) continue
            seen += product.id
            found += AlertMatch(
                alert = alert,
                product = product,
                postedMinutesAgo = product.createdAtMillis?.let { ((nowMillis - it) / 60_000).toInt() } ?: 0,
                reserved = product.id in reserved,
            )
        }
    }
    found.sortBy { it.postedMinutesAgo }
    return found
}