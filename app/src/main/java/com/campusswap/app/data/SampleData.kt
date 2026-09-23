package com.campusswap.app.data

import com.campusswap.app.domain.GeoPoint

object SampleData {

    val currentSeller = Seller(
        id = "me",
        name = "Daniel Diab",
        isVerified = true,
        rating = null,
        reviewCount = 0,
        maskedEmail = "d••••••@gmail.com",
    )

    private val sellerMaria = Seller("s1", "Maria Gomez", true, 4.8, 23, "m••••@uni.edu")
    private val sellerJuan = Seller("s2", "Juan Restrepo", true, 4.5, 11, "j••••@uni.edu")
    private val sellerLuisa = Seller("s3", "Luisa Fernandez", true, 4.9, 41, "l••••@uni.edu")
    private val sellerCarlos = Seller("s4", "Carlos Pena", true, 3.9, 6, "c••••@uni.edu")

    val courses = listOf(
        Course("MATH-201", "Calculus II"),
        Course("PHYS-150", "General Physics Lab"),
        Course("CS-330", "Data Structures"),
        Course("CHEM-110", "General Chemistry"),
        Course("ENG-201", "Technical Writing"),
        Course("STAT-220", "Probability & Statistics"),
        Course("BIO-101", "Introductory Biology"),
        Course("ECO-140", "Microeconomics"),
    )

    val categories = Category.entries.toList()

    val products: List<Product> = listOf(
        Product(
            id = "p1",
            title = "TI-Nspire CX CAS Graphing Calculator",
            description = "Barely used graphing calculator, perfect for Calculus and Physics courses. Includes original charger and case. No scratches on the screen.",
            price = 320000.0,
            category = Category.CALCULATORS,
            course = courses[0],
            condition = Condition.LIKE_NEW,
            rating = 4.7,
            reviewCount = 12,
            seller = sellerMaria,
            imageSeed = 1,
        ),
        Product(
            id = "p2",
            title = "Calculus: Early Transcendentals, 9th Ed.",
            description = "Stewart's calculus textbook, used one semester. Some highlighting in the first three chapters, otherwise in great shape.",
            price = 145000.0,
            category = Category.TEXTBOOKS,
            course = courses[0],
            condition = Condition.GOOD,
            rating = 4.2,
            reviewCount = 8,
            seller = sellerJuan,
            imageSeed = 2,
        ),
        Product(
            id = "p3",
            title = "General Physics Lab Kit",
            description = "Complete lab kit for PHYS-150: calipers, spring set, protractor, and lab manual. Everything needed for the semester.",
            price = 98000.0,
            category = Category.LAB_SUPPLIES,
            course = courses[1],
            condition = Condition.GOOD,
            rating = 4.4,
            reviewCount = 5,
            seller = sellerLuisa,
            imageSeed = 3,
        ),
        Product(
            id = "p4",
            title = "Data Structures & Algorithms Notes (Full Semester)",
            description = "Handwritten and typed notes covering the entire CS-330 syllabus, including diagrams for trees, graphs, and hashing.",
            price = 25000.0,
            category = Category.NOTES,
            course = courses[2],
            condition = Condition.LIKE_NEW,
            rating = null,
            reviewCount = 0,
            seller = sellerCarlos,
            imageSeed = 4,
        ),
        Product(
            id = "p5",
            title = "Chemistry Safety Goggles + Lab Coat",
            description = "Standard-issue safety goggles and a size M lab coat, worn for one semester of CHEM-110.",
            price = 42000.0,
            category = Category.LAB_SUPPLIES,
            course = courses[3],
            condition = Condition.FAIR,
            rating = 4.0,
            reviewCount = 3,
            seller = sellerMaria,
            imageSeed = 5,
        ),
        Product(
            id = "p6",
            title = "Casio FX-991 Scientific Calculator",
            description = "Reliable scientific calculator, great for general engineering and math courses. Works perfectly.",
            price = 65000.0,
            category = Category.CALCULATORS,
            course = null,
            condition = Condition.GOOD,
            rating = 4.6,
            reviewCount = 19,
            seller = sellerJuan,
            imageSeed = 6,
        ),
        Product(
            id = "p7",
            title = "Technical Writing Style Guide",
            description = "Course-required style guide for ENG-201, unmarked and unused, still shrink-wrapped.",
            price = 38000.0,
            category = Category.TEXTBOOKS,
            course = courses[4],
            condition = Condition.LIKE_NEW,
            rating = null,
            reviewCount = 0,
            seller = currentSeller,
            imageSeed = 7,
        ),
        Product(
            id = "p8",
            title = "Scientific Notebook Bundle (x3)",
            description = "Three graph-ruled notebooks, ideal for lab reports or problem sets. Never used.",
            price = 18000.0,
            category = Category.SUPPLIES,
            course = null,
            condition = Condition.LIKE_NEW,
            rating = 4.9,
            reviewCount = 7,
            seller = sellerLuisa,
            imageSeed = 8,
        ),
        Product(
            id = "p9",
            title = "Probability & Statistics Solutions Manual",
            description = "Companion solutions manual for STAT-220, covers all odd-numbered exercises with full derivations.",
            price = 52000.0,
            category = Category.TEXTBOOKS,
            course = courses[5],
            condition = Condition.GOOD,
            rating = 4.1,
            reviewCount = 4,
            seller = sellerCarlos,
            imageSeed = 9,
        ),
        Product(
            id = "p10",
            title = "USB Microscope for Biology Lab",
            description = "Digital USB microscope used for BIO-101 lab sessions, includes slide set and calibration tool.",
            price = 210000.0,
            category = Category.ELECTRONICS,
            course = courses[6],
            condition = Condition.GOOD,
            rating = 4.3,
            reviewCount = 9,
            seller = sellerMaria,
            imageSeed = 10,
        ),
        Product(
            id = "p11",
            title = "Microeconomics Flashcard Deck",
            description = "200-card deck covering every key term and graph from ECO-140. Laminated for durability.",
            price = 15000.0,
            category = Category.NOTES,
            course = courses[7],
            condition = Condition.LIKE_NEW,
            rating = 4.8,
            reviewCount = 15,
            seller = sellerJuan,
            imageSeed = 11,
        ),
        Product(
            id = "p12",
            title = "Drafting Kit for Engineering Drawing",
            description = "Complete drafting set: compass, rulers, set squares, and a scale ruler. Light wear, fully functional.",
            price = 60000.0,
            category = Category.SUPPLIES,
            course = null,
            condition = Condition.FAIR,
            rating = 3.8,
            reviewCount = 2,
            seller = sellerLuisa,
            imageSeed = 12,
        ),
    )

    val alerts = listOf(
        SmartAlert(
            id = "a1",
            keyword = "Casio FX-991",
            course = null,
            category = Category.CALCULATORS,
            maxPrice = 80000.0,
            minCondition = Condition.GOOD,
        ),
        SmartAlert(
            id = "a2",
            keyword = "",
            course = courses[2],
            category = Category.ALL,
            maxPrice = 120000.0,
            minCondition = Condition.GOOD,
            instant = false,
        ),
    )

    val notifications = listOf(
        AppNotification("n1", "New message", "Maria Gomez replied about the TI-Nspire calculator.", isRead = false, kind = NotificationKind.CHAT, productId = "p1"),
        AppNotification("n2", "Price drop", "A calculator on your wishlist dropped in price.", isRead = false, kind = NotificationKind.PRODUCT, productId = "p1"),
        AppNotification("n3", "Listing published", "Your listing \"Technical Writing Style Guide\" is now live.", isRead = true),
    )


    // TODO(carla): verificar en Google Maps que cada punto cae en la entrada correcta.
    private val centralLibraryLocation: GeoPoint? = GeoPoint(4.602948, -74.064829)
    private val studentCenterLocation: GeoPoint? = null // TODO(carla): coordenadas — Centro Cívico is not mapped in OSM yet
    private val marioLasernaLocation: GeoPoint? = GeoPoint(4.602725, -74.064696)
    private val plazoletaLlerasLocation: GeoPoint? = GeoPoint(4.601859, -74.065176)

    // Pre-mapped, public, monitored campus zones used by the Campus Guardian CAS (PAS 6 / PAS 10).
    // Walk times are relative to the two parties' last known campus micro-location.
    val meetingPoints = listOf(
        MeetingPoint("mp1", "Central Library lobby", "Main entrance, next to the security desk", MeetingZoneType.LIBRARY, isMonitored = true, walkMinutesMe = 4, walkMinutesOther = 5, mapX = 0.50f, mapY = 0.42f, location = centralLibraryLocation),
        MeetingPoint("mp2", "Student Center plaza", "Open plaza by the food court", MeetingZoneType.STUDENT_CENTER, isMonitored = true, walkMinutesMe = 7, walkMinutesOther = 3, mapX = 0.74f, mapY = 0.68f, location = studentCenterLocation),
        MeetingPoint("mp3", "Mario Laserna lobby", "Ground floor, engineering building", MeetingZoneType.BUILDING_LOBBY, isMonitored = true, walkMinutesMe = 2, walkMinutesOther = 9, mapX = 0.33f, mapY = 0.27f, location = marioLasernaLocation),
        MeetingPoint("mp4", "Plazoleta Lleras", "Open area in front of Lleras building", MeetingZoneType.PLAZA, isMonitored = false, walkMinutesMe = 6, walkMinutesOther = 6, mapX = 0.40f, mapY = 0.78f, location = plazoletaLlerasLocation),
    )

    // Shared free hours are computed from both parties' class schedules; other slots are plain suggestions.
    val timeSlots = listOf(
        TimeSlot("t1", "10:00 – 11:00", "Wed 16 Sep", isSharedBreak = true),
        TimeSlot("t2", "13:00 – 14:00", "Wed 16 Sep", isSharedBreak = false),
        TimeSlot("t3", "09:00 – 10:00", "Thu 17 Sep", isSharedBreak = true),
        TimeSlot("t4", "15:30 – 16:30", "Thu 17 Sep", isSharedBreak = false),
    )

    /** Seed conversation shown when a buyer opens a chat for the first time. */
    fun initialThread(product: Product): List<ChatMessage> = listOf(
        ChatMessage("c1", MessageAuthor.SYSTEM, "Chat started about \"${product.title}\". Your phone number is never shared.", "10:31"),
        ChatMessage("c2", MessageAuthor.ME, "Hi ${product.seller.name.substringBefore(' ')}! Is this still available?", "10:32"),
        ChatMessage("c3", MessageAuthor.OTHER, "Hey! Yes it is. It's in the condition shown in the photos, I only used it one semester.", "10:34"),
        ChatMessage("c4", MessageAuthor.ME, "Great. Could you do ${formatShortPrice(product.price * 0.9)}? I can pick it up on campus this week.", "10:36"),
        ChatMessage("c5", MessageAuthor.OTHER, "Sure, that works for me. Let's agree on a spot and a time.", "10:38"),
    )

    val cannedReplies = listOf(
        "Sounds good!",
        "Perfect, see you there.",
        "Yes, I'll bring it in its original case.",
        "Let me check my schedule and confirm.",
    )

    private fun formatShortPrice(amount: Double): String =
        "$" + java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "CO")).format(Math.round(amount / 1000.0) * 1000)

    fun productById(id: String): Product? = products.find { it.id == id }
}
