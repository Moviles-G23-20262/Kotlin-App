package com.campusswap.app.data.materials

import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.Course
import com.campusswap.app.data.Product
import com.campusswap.app.data.Seller
import com.campusswap.app.data.local.CachedMaterialEntity
import kotlin.math.abs

fun MaterialDto.toProduct(): Product = Product(
    id = id,
    title = title,
    description = description,
    price = price.toDoubleOrNull() ?: 0.0,
    category = category.toAppCategory(),
    course = courseCode?.takeIf { it.isNotBlank() }?.let { Course(code = it, name = it) },
    condition = condition.toAppCondition(),
    rating = null,
    reviewCount = 0,
    seller = Seller(
        id = sellerId,
        name = seller?.fullName ?: "CampusSwap student",
        isVerified = seller != null,
        rating = seller?.rating?.takeIf { it > 0 },
        reviewCount = 0,
        maskedEmail = seller?.email?.let(::maskEmail).orEmpty(),
    ),
    imageSeed = abs(id.hashCode()) % 12,
)

private fun String.toAppCategory(): Category = when (this) {
    "BOOKS" -> Category.TEXTBOOKS
    "CALCULATORS" -> Category.CALCULATORS
    "LAB_EQUIPMENT" -> Category.LAB_SUPPLIES
    else -> Category.SUPPLIES
}

private fun String?.toAppCondition(): Condition = when (this) {
    "NEW", "LIKE_NEW" -> Condition.LIKE_NEW
    "FAIR" -> Condition.FAIR
    else -> Condition.GOOD
}

private fun maskEmail(email: String): String {
    val (user, domain) = email.split("@", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return "${user.take(2)}***@$domain"
}
fun MaterialDto.toCachedEntity(position: Int, cachedAtMillis: Long) = CachedMaterialEntity(
    id = id,
    title = title,
    description = description,
    courseCode = courseCode,
    price = price,
    condition = condition,
    status = status,
    category = category,
    sellerId = sellerId,
    sellerName = seller?.fullName,
    sellerEmail = seller?.email,
    sellerRating = seller?.rating,
    position = position,
    cachedAtMillis = cachedAtMillis,
)

fun CachedMaterialEntity.toDto() = MaterialDto(
    id = id,
    title = title,
    description = description,
    courseCode = courseCode,
    price = price,
    condition = condition,
    status = status,
    category = category,
    sellerId = sellerId,
    seller = if (sellerName != null && sellerEmail != null) MaterialSellerDto(sellerId, sellerEmail, sellerName, sellerRating) else null,
)

/** Server enums for a listing written in the app. */
fun Category.toServerCategory(): String = when (this) {
    Category.TEXTBOOKS, Category.NOTES -> "BOOKS"
    Category.CALCULATORS -> "CALCULATORS"
    Category.LAB_SUPPLIES -> "LAB_EQUIPMENT"
    else -> "OTHER"
}

fun Condition.toServerCondition(): String = name
