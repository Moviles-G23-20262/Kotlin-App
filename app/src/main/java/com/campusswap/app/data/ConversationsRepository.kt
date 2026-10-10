package com.campusswap.app.data

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
    data class Success(val conversations: List<Conversation>) : ConversationsLoad
    data object Offline : ConversationsLoad
}

class ConversationsRepository(
    private val remote: ChatRoomRemoteDataSource,
    private val currentUserId: () -> String?,
    private val zone: ZoneId = MeetingProposalMapper.CAMPUS_ZONE,
) {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.US)

    suspend fun conversations(): ConversationsLoad = try {
        ConversationsLoad.Success(
            remote.rooms()
                .map(::toConversation)
                .sortedByDescending { it.lastMessageAt ?: Instant.EPOCH },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        ConversationsLoad.Offline
    } catch (e: HttpException) {
        ConversationsLoad.Offline
    }

    suspend fun unreadCount(): Int = when (val loaded = conversations()) {
        is ConversationsLoad.Success -> loaded.conversations.sumOf { it.unread }
        ConversationsLoad.Offline -> 0
    }

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
        )
    }

    private fun label(sentAt: Instant): String {
        val at = sentAt.atZone(zone)
        val today = Instant.now().atZone(zone).toLocalDate()
        return if (at.toLocalDate() == today) timeFormat.format(at) else dayFormat.format(at)
    }
}
