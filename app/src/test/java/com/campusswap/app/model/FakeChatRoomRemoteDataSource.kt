package com.campusswap.app.data

import com.campusswap.app.data.remote.ChatRoomRemoteDataSource
import com.campusswap.app.data.remote.ChatRoomSummaryDto

class FakeChatRoomRemoteDataSource : ChatRoomRemoteDataSource {
    var failure: Exception? = null
    val openedMaterials = mutableListOf<String>()
    val readRooms = mutableListOf<String>()
    var summaries = mutableListOf<ChatRoomSummaryDto>()

    var openCalls = 0

    override suspend fun open(materialId: String): String {
        openCalls++
        failure?.let { throw it }
        openedMaterials += materialId
        return "room-1"
    }

    override suspend fun markRead(chatRoomId: String) {
        failure?.let { throw it }
        readRooms += chatRoomId
    }

    override suspend fun rooms(): List<ChatRoomSummaryDto> {
        failure?.let { throw it }
        return summaries
    }
}
