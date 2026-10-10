package com.campusswap.app.data

object SeedIds {
    private val users = mapOf("me" to 0, "s1" to 1, "s2" to 2, "s3" to 3, "s4" to 4)
    private val materials = (1..12).associateBy { "p$it" }
    private val meetingPoints = (1..4).associateBy { "mp$it" }

    private val UUID_PATTERN =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun user(localId: String): String? = users[localId]?.let { uuid('a', it) }

    fun material(localId: String): String? = materials[localId]?.let { uuid('b', it) }

    fun meetingPoint(localId: String): String? = meetingPoints[localId]?.let { uuid('c', it) }

    fun isBackendId(id: String): Boolean = UUID_PATTERN.matches(id)

    fun backendMaterial(productId: String): String? =
        material(productId) ?: productId.takeIf(::isBackendId)

    fun backendMeetingPoint(pointId: String): String? =
        meetingPoint(pointId) ?: pointId.takeIf(::isBackendId)

    fun backendUser(userId: String): String? =
        user(userId) ?: userId.takeIf(::isBackendId)

    private fun uuid(prefix: Char, n: Int) = "${prefix}0000000-0000-4000-8000-%012x".format(n)
}
