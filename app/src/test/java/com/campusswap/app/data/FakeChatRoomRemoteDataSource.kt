package com.campusswap.app.data

import com.campusswap.app.data.remote.ChatRoomRemoteDataSource

class FakeChatRoomRemoteDataSource : ChatRoomRemoteDataSource {
    var failure: Exception? = null
    val openedMaterials = mutableListOf<String>()
    val readRooms = mutableListOf<String>()

    override suspend fun open(materialId: String): String {
        failure?.let { throw it }
        openedMaterials += materialId
        return "room-1"
    }

    override suspend fun markRead(chatRoomId: String) {
        failure?.let { throw it }
        readRooms += chatRoomId
    }
}
