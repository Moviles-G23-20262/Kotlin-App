package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatRoomsApi {
    @POST("chatrooms")
    suspend fun open(@Body body: OpenChatRoomRequest): ChatRoomDto

    @POST("chatrooms/{id}/read")
    suspend fun markRead(@Path("id") chatRoomId: String)
}

data class OpenChatRoomRequest(val materialId: String)

data class ChatRoomDto(val id: String)

interface ChatRoomRemoteDataSource {
    suspend fun open(materialId: String): String
    suspend fun markRead(chatRoomId: String)
}

class RetrofitChatRoomRemoteDataSource(private val api: ChatRoomsApi) : ChatRoomRemoteDataSource {
    override suspend fun open(materialId: String) = api.open(OpenChatRoomRequest(materialId)).id
    override suspend fun markRead(chatRoomId: String) = api.markRead(chatRoomId)
}
