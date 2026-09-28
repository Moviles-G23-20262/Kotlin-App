package com.campusswap.app.data

import com.campusswap.app.data.remote.PopularityRemoteDataSource
import kotlinx.coroutines.CancellationException

class MeetingPointPopularityRepository(private val remote: PopularityRemoteDataSource) {

    suspend fun popularAt(hour: Int): Set<String> = try {
        val usage = remote.usageAt(hour)
        if (!usage.available) {
            emptySet()
        } else {
            usage.data.orEmpty()
                .filter { it.total > 0 }
                .sortedByDescending { it.total }
                .take(TOP_POINTS)
                .map { it.meetingPointId }
                .toSet()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptySet()
    }

    companion object {
        const val TOP_POINTS = 3
    }
}
