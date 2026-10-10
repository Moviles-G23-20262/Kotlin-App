package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MessagesApi {
    @GET("messages")
    suspend fun messages(@Query("chatRoomId") chatRoomId: String): List<MessageDto>

    @POST("messages")
    suspend fun send(@Body body: CreateMessageRequest): MessageDto
}

data class CreateMessageRequest(val chatRoomId: String, val content: String)

data class MessageSenderDto(val id: String, val fullName: String?)

data class MessageDto(
    val id: String,
    val chatRoomId: String,
    val senderId: String,
    val content: String,
    val type: String?,
    val isRead: Boolean?,
    val createdAt: String,
    val sender: MessageSenderDto?,
    val meetingProposal: MeetingProposalDto?,
)

interface MessageRemoteDataSource {
    suspend fun messages(chatRoomId: String): List<MessageDto>
    suspend fun send(chatRoomId: String, content: String): MessageDto
}

class RetrofitMessageRemoteDataSource(private val api: MessagesApi) : MessageRemoteDataSource {
    override suspend fun messages(chatRoomId: String) = api.messages(chatRoomId)
    override suspend fun send(chatRoomId: String, content: String) = api.send(CreateMessageRequest(chatRoomId, content))
}
