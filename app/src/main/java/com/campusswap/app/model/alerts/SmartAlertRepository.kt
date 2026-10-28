package com.campusswap.app.model.alerts

import com.campusswap.app.model.Category
import com.campusswap.app.model.Condition
import com.campusswap.app.model.Course
import com.campusswap.app.model.SmartAlert
import com.campusswap.app.model.local.ResponseCache
import com.campusswap.app.model.local.load
import com.campusswap.app.model.local.save
import com.campusswap.app.model.materials.toAppCategory
import com.campusswap.app.model.materials.toServerCategory
import kotlinx.coroutines.CancellationException

/** [isLive] is false when the server couldn't be reached and the alerts are the copy saved on the phone. */
data class SmartAlertsFeed(val alerts: List<SmartAlert>, val isLive: Boolean)

/**
 * The student's saved searches, kept on the server so the server can notify a match the moment
 * someone publishes it (Smart Matching), even with the app closed.
 */
class SmartAlertRepository(
    private val remote: SmartAlertRemoteDataSource,
    private val currentUserId: () -> String?,
    private val cache: ResponseCache = ResponseCache.None,
) {
    /** null when offline and nothing was saved before. */
    suspend fun load(): SmartAlertsFeed? = try {
        val alerts = remote.getAll()
        currentUserId()?.let { cache.save(it, KEY, alerts) }
        SmartAlertsFeed(alerts.map { it.toSmartAlert() }, isLive = true)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        currentUserId()
            ?.let { cache.load<List<SmartAlertDto>>(it, KEY) }
            ?.let { saved -> SmartAlertsFeed(saved.value.map { it.toSmartAlert() }, isLive = false) }
    }

    /** The alert as the server saved it (with its real id), or null if it couldn't be saved. */
    suspend fun create(alert: SmartAlert): SmartAlert? = attempt { remote.create(alert.toRequest()).toSmartAlert() }

    suspend fun setEnabled(id: String, enabled: Boolean): Boolean = attempt { remote.setEnabled(id, enabled) } != null

    suspend fun remove(id: String): Boolean = attempt { remote.remove(id) } != null

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val KEY = "smart-alerts"
    }
}

internal fun SmartAlert.toRequest() = CreateSmartAlertRequest(
    keyword = keyword.trim(),
    courseCode = course?.code,
    category = category.takeUnless { it == Category.ALL }?.toServerCategory(),
    maxPrice = maxPrice.toLong().toString(),
    // FAIR is the worst condition there is, so it means "any".
    minCondition = minCondition.takeUnless { it == Condition.FAIR }?.name,
    enabled = enabled,
)

internal fun SmartAlertDto.toSmartAlert() = SmartAlert(
    id = id,
    keyword = keyword.orEmpty(),
    course = courseCode?.let { Course(it, it) },
    category = category?.toAppCategory() ?: Category.ALL,
    maxPrice = maxPrice.toDoubleOrNull() ?: 0.0,
    minCondition = Condition.entries.firstOrNull { it.name == minCondition } ?: Condition.FAIR,
    enabled = enabled,
)
