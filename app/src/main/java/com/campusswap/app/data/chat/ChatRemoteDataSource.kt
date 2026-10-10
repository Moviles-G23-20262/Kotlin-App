package com.campusswap.app.data.chat

import retrofit2.http.Body
import retrofit2.http.POST

data class CreateChatRoomRequest(val materialId: String)

data class ChatRoomDto(val id: String)

data class CreateMessageRequest(val chatRoomId: String, val content: String)

data class MessageDto(val id: String)

interface ChatApi {
    /** Returns the existing room when the buyer already has one for this listing. */
    @POST("chatrooms")
    suspend fun openRoom(@Body body: CreateChatRoomRequest): ChatRoomDto

    @POST("messages")
    suspend fun send(@Body body: CreateMessageRequest): MessageDto
}

interface ChatRemoteDataSource {
    suspend fun openRoom(materialId: String): String
    suspend fun send(chatRoomId: String, content: String)
}

class RetrofitChatRemoteDataSource(private val api: ChatApi) : ChatRemoteDataSource {
    override suspend fun openRoom(materialId: String): String = api.openRoom(CreateChatRoomRequest(materialId)).id
    override suspend fun send(chatRoomId: String, content: String) {
        api.send(CreateMessageRequest(chatRoomId, content))
    }
}
