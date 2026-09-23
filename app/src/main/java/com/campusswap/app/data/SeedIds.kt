package com.campusswap.app.data

object SeedIds {
    private val users = mapOf("me" to 0, "s1" to 1, "s2" to 2, "s3" to 3, "s4" to 4)
    private val materials = (1..12).associateBy { "p$it" }
    private val meetingPoints = (1..4).associateBy { "mp$it" }

    fun user(localId: String): String? = users[localId]?.let { uuid('a', it) }

    fun material(localId: String): String? = materials[localId]?.let { uuid('b', it) }

    fun meetingPoint(localId: String): String? = meetingPoints[localId]?.let { uuid('c', it) }

    private fun uuid(prefix: Char, n: Int) = "${prefix}0000000-0000-4000-8000-%012x".format(n)
}
