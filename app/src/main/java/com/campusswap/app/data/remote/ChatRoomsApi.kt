package com.campusswap.app.data.remote

import com.campusswap.app.data.materials.MaterialDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatRoomsApi {
    @POST("chatrooms")
    suspend fun open(@Body body: OpenChatRoomRequest): ChatRoomDto

    @GET("chatrooms")
    suspend fun rooms(): List<ChatRoomSummaryDto>

    @POST("chatrooms/{id}/read")
    suspend fun markRead(@Path("id") chatRoomId: String)
}

data class OpenChatRoomRequest(val materialId: String)

data class ChatRoomDto(val id: String)

data class ChatPartyDto(val id: String, val fullName: String?)

data class UnreadCountDto(val messages: Int?)

data class ChatRoomSummaryDto(
    val id: String,
    val materialId: String,
    val buyerId: String,
    val sellerId: String,
    val material: MaterialDto?,
    val buyer: ChatPartyDto?,
    val seller: ChatPartyDto?,
    val messages: List<MessageDto>?,
    @SerializedName("_count") val count: UnreadCountDto?,
)

interface ChatRoomRemoteDataSource {
    suspend fun open(materialId: String): String
    suspend fun markRead(chatRoomId: String)
    suspend fun rooms(): List<ChatRoomSummaryDto>
}

class RetrofitChatRoomRemoteDataSource(private val api: ChatRoomsApi) : ChatRoomRemoteDataSource {
    override suspend fun open(materialId: String) = api.open(OpenChatRoomRequest(materialId)).id
    override suspend fun markRead(chatRoomId: String) = api.markRead(chatRoomId)
    override suspend fun rooms() = api.rooms()
}
