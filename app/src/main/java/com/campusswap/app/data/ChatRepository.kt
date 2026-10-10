package com.campusswap.app.data

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
    data class Success(val messages: List<ChatMessage>) : ChatLoad
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
) {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    suspend fun messages(productId: String): ChatLoad = try {
        val room = rooms.roomFor(productId)
        if (room == null) ChatLoad.NotSynced else ChatLoad.Success(remote.messages(room).map(::toChatMessage))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ChatLoad.Offline
    } catch (e: HttpException) {
        ChatLoad.Offline
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

    suspend fun counterpartName(productId: String): String? {
        val summary = rooms.summaryFor(productId) ?: return null
        val mine = currentUserId()
        return (if (summary.sellerId == mine) summary.buyer else summary.seller)?.fullName
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

    private companion object {
        val PHOTO_SUFFIXES = listOf(".jpg", ".jpeg", ".png", ".webp")
    }
}
