package com.campusswap.app.data

import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save
import com.campusswap.app.data.remote.ApiErrors
import com.campusswap.app.data.remote.MessageDto
import com.campusswap.app.data.remote.MessageRemoteDataSource
import com.campusswap.app.data.remote.UploadRemoteDataSource
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ChatLoad {
    /** [savedAtMillis] is set when the server couldn't be reached and this is the copy saved on the phone. */
    data class Success(val messages: List<ChatMessage>, val savedAtMillis: Long? = null) : ChatLoad
    data object NotSynced : ChatLoad
    data object Offline : ChatLoad
}

sealed interface SendResult {
    data class Success(val message: ChatMessage) : SendResult
    data object NotSynced : SendResult
    data object Offline : SendResult
    data class Rejected(val message: String?) : SendResult
}

class ChatRepository(
    private val rooms: ChatRoomRepository,
    private val remote: MessageRemoteDataSource,
    private val uploads: UploadRemoteDataSource,
    private val currentUserId: () -> String?,
    private val zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE,
    private val cache: ResponseCache = ResponseCache.None,
) {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    suspend fun messages(productId: String): ChatLoad = try {
        val room = rooms.roomFor(productId)
        if (room == null) {
            ChatLoad.NotSynced
        } else {
            val thread = remote.messages(room)
            currentUserId()?.let { cache.save(it, threadKey(productId), thread) }
            ChatLoad.Success(thread.map(::toChatMessage))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        savedThread(productId) ?: ChatLoad.Offline
    } catch (e: HttpException) {
        savedThread(productId) ?: ChatLoad.Offline
    }

    private suspend fun savedThread(productId: String): ChatLoad? {
        val owner = currentUserId() ?: return null
        val saved = cache.load<List<MessageDto>>(owner, threadKey(productId)) ?: return null
        return ChatLoad.Success(saved.value.map(::toChatMessage), saved.savedAtMillis)
    }

    suspend fun send(productId: String, text: String): SendResult = try {
        val room = rooms.roomFor(productId)
        if (room == null) SendResult.NotSynced else SendResult.Success(toChatMessage(remote.send(room, text)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        SendResult.Offline
    } catch (e: HttpException) {
        SendResult.Rejected(ApiErrors.message(e))
    }

    suspend fun uploadPhoto(bytes: ByteArray, mimeType: String): String? = try {
        uploads.upload(bytes, mimeType)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun markRead(productId: String) = rooms.markRead(productId)

    /** The server's chat room for the listing, or null when it can't be reached. */
    suspend fun roomId(productId: String): String? = try {
        rooms.roomFor(productId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    suspend fun counterpartName(productId: String): String? {
        val mine = currentUserId()
        val summary = rooms.summaryFor(productId)
            ?: return mine?.let { cache.load<String>(it, counterpartKey(productId))?.value }
        val name = (if (summary.sellerId == mine) summary.buyer else summary.seller)?.fullName
        if (mine != null && name != null) cache.save(mine, counterpartKey(productId), name)
        return name
    }

    fun latestProposal(messages: List<ChatMessage>): MeetingProposal? =
        messages.lastOrNull { it.proposal != null }?.proposal

    private fun toChatMessage(dto: MessageDto): ChatMessage {
        val proposal = dto.meetingProposal?.let { MeetingProposalMapper.toProposal(it, zone) }
        val mine = dto.senderId == currentUserId()
        val sentAt = runCatching { Instant.parse(dto.createdAt) }.getOrNull()
        val photo = dto.content.takeIf(::isPhoto)
        return ChatMessage(
            id = dto.id,
            author = if (proposal != null) MessageAuthor.SYSTEM else if (mine) MessageAuthor.ME else MessageAuthor.OTHER,
            text = dto.content,
            time = sentAt?.let { timeFormat.format(it.atZone(zone)) }.orEmpty(),
            status = if (dto.isRead == true) MessageStatus.READ else MessageStatus.DELIVERED,
            proposal = proposal,
            sentAt = sentAt,
            imageUrl = photo,
        )
    }

    private fun isPhoto(content: String) =
        content.startsWith("https://") && PHOTO_SUFFIXES.any { content.substringBefore('?').endsWith(it, ignoreCase = true) }

    private fun threadKey(productId: String) = "chat-thread:$productId"

    private fun counterpartKey(productId: String) = "chat-counterpart:$productId"

    private companion object {
        val PHOTO_SUFFIXES = listOf(".jpg", ".jpeg", ".png", ".webp")
    }
}
