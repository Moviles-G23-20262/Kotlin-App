package com.campusswap.app.data

enum class Condition(val label: String) {
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair"),
}

enum class Category(val label: String) {
    ALL("All"),
    CALCULATORS("Calculators"),
    TEXTBOOKS("Textbooks"),
    LAB_SUPPLIES("Lab Supplies"),
    SUPPLIES("Supplies"),
    NOTES("Notes"),
    ELECTRONICS("Electronics"),
}

enum class SortOption(val label: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW("Price: low to high"),
    PRICE_HIGH("Price: high to low"),
    RATING("Rating"),
}

data class Course(
    val code: String,
    val name: String,
)

data class Seller(
    val id: String,
    val name: String,
    val isVerified: Boolean,
    val rating: Double?,
    val reviewCount: Int,
    val maskedEmail: String,
)

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: Category,
    val course: Course?,
    val condition: Condition,
    val rating: Double?,
    val reviewCount: Int,
    val seller: Seller,
    val imageSeed: Int,
)

data class CartLine(
    val product: Product,
    val quantity: Int,
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
)
