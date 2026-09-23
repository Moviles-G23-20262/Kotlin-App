package com.campusswap.app.data

import com.campusswap.app.domain.GeoPoint

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

enum class NotificationKind { GENERIC, CHAT, PRODUCT, ALERT_MATCH, EXCHANGE }

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val kind: NotificationKind = NotificationKind.GENERIC,
    val productId: String? = null,
)

// ---- In-app chat (View 09) ----

enum class MessageStatus { SENDING, SENT, DELIVERED, READ }

enum class MessageAuthor { ME, OTHER, SYSTEM }

data class ChatMessage(
    val id: String,
    val author: MessageAuthor,
    val text: String,
    val time: String,
    val status: MessageStatus = MessageStatus.READ,
    /** Attached meeting proposal, rendered as a system card inside the thread. */
    val proposal: MeetingProposal? = null,
)

// ---- Dynamic safe meeting points (View 10, "Campus Guardian" CAS) ----

enum class MeetingZoneType(val label: String) {
    LIBRARY("Library"),
    STUDENT_CENTER("Student center"),
    BUILDING_LOBBY("Building lobby"),
    PLAZA("Open plaza"),
}

data class MeetingPoint(
    val id: String,
    val name: String,
    val detail: String,
    val zoneType: MeetingZoneType,
    val isMonitored: Boolean,
    val walkMinutesMe: Int,
    val walkMinutesOther: Int,
    /** Normalised 0..1 position on the stylised campus map. */
    val mapX: Float,
    val mapY: Float,
    /** Real-world position used for GPS check-in; null until surveyed. */
    val location: GeoPoint? = null,
)

data class TimeSlot(
    val id: String,
    val label: String,
    val day: String,
    /** True when the slot is a shared free hour between both parties' class schedules. */
    val isSharedBreak: Boolean,
)

enum class ProposalStatus(val label: String) {
    PENDING("Awaiting confirmation"),
    ACCEPTED("Confirmed by both"),
    CHANGED("Alternative proposed"),
}

data class MeetingProposal(
    val point: MeetingPoint,
    val slot: TimeSlot,
    val status: ProposalStatus,
)

// saved searches & smart matching (View 11)

/** A saved search that notifies the student when a matching listing is posted. */
data class SmartAlert(
    val id: String,
    val keyword: String,
    val course: Course?,
    val category: Category,
    val maxPrice: Double,
    val minCondition: Condition,
    val instant: Boolean = true,
    val quietDuringClasses: Boolean = true,
    val enabled: Boolean = true,
)

/** A listing that satisfies an alert */
data class AlertMatch(
    val alert: SmartAlert,
    val product: Product,
    val postedMinutesAgo: Int,
    val reserved: Boolean = false,
) {
    val savingPercent: Int
        get() = (((alert.maxPrice - product.price) / alert.maxPrice) * 100).toInt().coerceAtLeast(0)
}

// transaction completion and rating (View 12)

enum class CheckItem(val label: String) {
    MATCHES_PHOTOS("Matches the photos"),
    CONDITION_OK("Condition is as listed"),
    COMPLETE("Everything included"),
    WORKS("Works as expected"),
}

enum class FeedbackTag(val label: String) {
    PUNCTUAL("Punctual"),
    AS_DESCRIBED("Item as described"),
    FRIENDLY("Friendly"),
    GOOD_COMMS("Good communication"),
    FAIR_PRICE("Fair price"),
}

data class TransactionRating(
    val productId: String,
    val stars: Int,
    val confirmedCondition: Condition,
    val tags: Set<FeedbackTag>,
    val review: String,
    /** Ratings stay hidden until the other party rates */
    val revealed: Boolean = false,
)


data class Order(
    val number: Int,
    val lines: List<CartLine>,
    /** True when the buyer chose to meet on campus instead of home delivery. */
    val meetOnCampus: Boolean = true,
) {
    val products: List<Product> get() = lines.map { it.product }
    val total: Double get() = lines.sumOf { it.product.price * it.quantity }
}

data class PendingExchange(
    val order: Order,
    val product: Product,
    val proposal: MeetingProposal?,
    val rating: TransactionRating?,
) {
    val isRated: Boolean get() = rating != null
    val isScheduled: Boolean get() = proposal?.status == ProposalStatus.ACCEPTED
}
