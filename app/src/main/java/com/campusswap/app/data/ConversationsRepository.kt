package com.campusswap.app.data

import com.campusswap.app.data.local.ResponseCache
import com.campusswap.app.data.local.load
import com.campusswap.app.data.local.save
import com.campusswap.app.data.remote.ChatRoomRemoteDataSource
import com.campusswap.app.data.remote.ChatRoomSummaryDto
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed interface ConversationsLoad {
    /** [savedAtMillis] is set when the server couldn't be reached and this is the copy saved on the phone. */
    data class Success(val conversations: List<Conversation>, val savedAtMillis: Long? = null) : ConversationsLoad
    data object Offline : ConversationsLoad
}

class ConversationsRepository(
    private val remote: ChatRoomRemoteDataSource,
    private val currentUserId: () -> String?,
    private val zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE,
    private val cache: ResponseCache = ResponseCache.None,
) {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.US)

    suspend fun conversations(): ConversationsLoad = try {
        val rooms = remote.rooms()
        currentUserId()?.let { cache.save(it, INBOX_KEY, rooms) }
        ConversationsLoad.Success(sorted(rooms))
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        savedInbox() ?: ConversationsLoad.Offline
    } catch (e: HttpException) {
        savedInbox() ?: ConversationsLoad.Offline
    }

    /** Only counts what the server says now: an old copy would show messages that were already read. */
    suspend fun unreadCount(): Int = when (val loaded = conversations()) {
        is ConversationsLoad.Success -> if (loaded.savedAtMillis == null) loaded.conversations.sumOf { it.unread } else 0
        ConversationsLoad.Offline -> 0
    }

    private suspend fun savedInbox(): ConversationsLoad? {
        val owner = currentUserId() ?: return null
        val saved = cache.load<List<ChatRoomSummaryDto>>(owner, INBOX_KEY) ?: return null
        return ConversationsLoad.Success(sorted(saved.value), saved.savedAtMillis)
    }

    private fun sorted(rooms: List<ChatRoomSummaryDto>) =
        rooms.map(::toConversation).sortedByDescending { it.lastMessageAt ?: Instant.EPOCH }

    private fun toConversation(room: ChatRoomSummaryDto): Conversation {
        val selling = room.sellerId == currentUserId()
        val counterpart = if (selling) room.buyer else room.seller
        val last = room.messages?.firstOrNull()
        val sentAt = last?.let { runCatching { Instant.parse(it.createdAt) }.getOrNull() }
        return Conversation(
            productId = room.materialId,
            productTitle = room.material?.title ?: "This listing",
            counterpartName = counterpart?.fullName ?: "A student",
            lastMessage = last?.content,
            lastMessageAt = sentAt,
            time = sentAt?.let(::label).orEmpty(),
            unread = room.count?.messages ?: 0,
            sellingThis = selling,
            roomId = room.id,
        )
    }

    private fun label(sentAt: Instant): String {
        val at = sentAt.atZone(zone)
        val today = Instant.now().atZone(zone).toLocalDate()
        return if (at.toLocalDate() == today) timeFormat.format(at) else dayFormat.format(at)
    }

    private companion object {
        const val INBOX_KEY = "chat-inbox"
    }
}
