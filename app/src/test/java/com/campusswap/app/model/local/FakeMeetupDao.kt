package com.campusswap.app.model.local

class FakeMeetupDao : MeetupDao {
    val rows = mutableListOf<MeetupEntity>()

    override suspend fun forOwner(ownerId: String) = rows.filter { it.ownerId == ownerId }.sortedBy { it.startsAtMillis }

    override suspend fun clear(ownerId: String) {
        rows.removeAll { it.ownerId == ownerId }
    }

    override suspend fun insertAll(items: List<MeetupEntity>) {
        rows += items
    }

    override suspend fun replace(ownerId: String, items: List<MeetupEntity>) {
        clear(ownerId)
        insertAll(items)
    }
}
