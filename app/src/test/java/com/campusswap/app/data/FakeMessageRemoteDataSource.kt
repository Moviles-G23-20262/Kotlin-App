package com.campusswap.app.data

import com.campusswap.app.data.remote.MessageDto
import com.campusswap.app.data.remote.MessageRemoteDataSource

class FakeMessageRemoteDataSource : MessageRemoteDataSource {
    var stored = mutableListOf<MessageDto>()
    var failure: Exception? = null
    val sent = mutableListOf<Pair<String, String>>()

    override suspend fun messages(chatRoomId: String): List<MessageDto> {
        failure?.let { throw it }
        return stored.filter { it.chatRoomId == chatRoomId }
    }

    override suspend fun send(chatRoomId: String, content: String): MessageDto {
        failure?.let { throw it }
        sent += chatRoomId to content
        return MessageDto(
            id = "m-${stored.size + 1}",
            chatRoomId = chatRoomId,
            senderId = ME,
            content = content,
            type = "TEXT",
            isRead = false,
            createdAt = "2026-10-05T17:00:00Z",
            sender = null,
            meetingProposal = null,
        )
    }

    companion object {
        const val ME = "a0000000-0000-4000-8000-000000000000"
        const val OTHER = "a0000000-0000-4000-8000-000000000001"
    }
}
