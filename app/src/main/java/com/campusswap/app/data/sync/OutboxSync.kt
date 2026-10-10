package com.campusswap.app.data.sync

import com.campusswap.app.data.Category
import com.campusswap.app.data.Condition
import com.campusswap.app.data.chat.ChatRemoteDataSource
import com.campusswap.app.data.materials.toServerCategory
import com.campusswap.app.data.materials.toServerCondition
import com.campusswap.app.data.local.OutboxDao
import com.campusswap.app.data.local.PendingListingEntity
import com.campusswap.app.data.materials.CreateMaterialRequest
import com.campusswap.app.data.materials.MaterialRemoteDataSource
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

enum class SyncOutcome { DONE, RETRY_LATER }

/**
 * Sends what the user did offline, oldest first. Each item has three possible endings:
 * delivered (removed from the outbox), "try again later" (no network, server down or session
 * expired: it stays queued) or refused by the server (kept with the reason so the UI can say so).
 */
class OutboxSync(
    private val outbox: OutboxDao,
    private val materials: MaterialRemoteDataSource,
    private val chat: ChatRemoteDataSource,
) {
    private sealed interface Attempt {
        data object Sent : Attempt
        data object Retry : Attempt
        data class Refused(val reason: String) : Attempt
    }

    suspend fun flush(ownerId: String): SyncOutcome {
        for (listing in outbox.listingsToSend(ownerId)) {
            when (val result = attempt { materials.create(listing.toRequest()) }) {
                Attempt.Sent -> outbox.deleteListing(listing.localId)
                Attempt.Retry -> {
                    outbox.markListingAttempt(listing.localId, null)
                    return SyncOutcome.RETRY_LATER
                }
                is Attempt.Refused -> outbox.markListingAttempt(listing.localId, result.reason)
            }
        }

        // One room per listing; opening it is idempotent on the server, so remembering it only saves calls.
        val rooms = mutableMapOf<String, String>()
        for (message in outbox.messagesToSend(ownerId)) {
            val result = attempt {
                val roomId = rooms.getOrPut(message.materialId) { chat.openRoom(message.materialId) }
                chat.send(roomId, message.content)
            }
            when (result) {
                Attempt.Sent -> outbox.deleteMessage(message.localId)
                // Stop here so later messages never arrive before an earlier one.
                Attempt.Retry -> {
                    outbox.markMessageAttempt(message.localId, null)
                    return SyncOutcome.RETRY_LATER
                }
                is Attempt.Refused -> outbox.markMessageAttempt(message.localId, result.reason)
            }
        }
        return SyncOutcome.DONE
    }

    private suspend fun attempt(block: suspend () -> Unit): Attempt = try {
        block()
        Attempt.Sent
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Attempt.Retry
    } catch (e: HttpException) {
        when (e.code()) {
            401, 408, 429, in 500..599 -> Attempt.Retry
            else -> Attempt.Refused(serverMessage(e) ?: "The server rejected it (${e.code()})")
        }
    }

    private fun serverMessage(e: HttpException): String? = runCatching {
        val message = JsonParser.parseString(e.response()?.errorBody()?.string().orEmpty()).asJsonObject.get("message")
        if (message.isJsonArray) message.asJsonArray.first().asString else message.asString
    }.getOrNull()
}

private fun PendingListingEntity.toRequest() = CreateMaterialRequest(
    title = title,
    description = description,
    courseCode = courseCode,
    price = price,
    condition = (Condition.entries.firstOrNull { it.name == condition } ?: Condition.GOOD).toServerCondition(),
    category = (Category.entries.firstOrNull { it.name == category } ?: Category.SUPPLIES).toServerCategory(),
)
