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
