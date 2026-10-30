package com.campusswap.app.model

import com.campusswap.app.model.local.ResponseCache
import com.campusswap.app.model.local.load
import com.campusswap.app.model.local.save
import com.campusswap.app.model.remote.MeetingPointUsageResponse
import com.campusswap.app.model.remote.PopularityRemoteDataSource
import kotlinx.coroutines.CancellationException

/** The busiest meeting points for an hour; [savedAtMillis] is set when they come from the copy on the phone. */
data class Popularity(val ids: Set<String>, val isLive: Boolean = true, val savedAtMillis: Long? = null)

/**
 * "Popular now" from BQ12. Offline it reuses the last answer saved for the same hour. The data is
 * the same for every student, so it is saved as shared.
 */
class MeetingPointPopularityRepository(
    private val remote: PopularityRemoteDataSource,
    private val cache: ResponseCache = ResponseCache.None,
) {

    suspend fun popularAt(hour: Int): Popularity = try {
        val usage = remote.usageAt(hour)
        if (usage.available) cache.save(ResponseCache.SHARED, key(hour), usage)
        Popularity(topPoints(usage))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val saved = cache.load<MeetingPointUsageResponse>(ResponseCache.SHARED, key(hour))
        Popularity(saved?.value?.let(::topPoints).orEmpty(), isLive = false, savedAtMillis = saved?.savedAtMillis)
    }

    private fun topPoints(usage: MeetingPointUsageResponse): Set<String> =
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

    private fun key(hour: Int) = "meeting_point_usage:$hour"

    companion object {
        const val TOP_POINTS = 3
    }
}
