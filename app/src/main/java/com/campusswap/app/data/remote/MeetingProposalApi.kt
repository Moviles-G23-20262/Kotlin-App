package com.campusswap.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MeetingProposalApi {
    @GET("meeting-proposals/suggestions")
    suspend fun suggestions(@Query("chatRoomId") chatRoomId: String): SlotSuggestionsDto

    @GET("meeting-proposals")
    suspend fun proposals(@Query("chatRoomId") chatRoomId: String): List<MeetingProposalDto>

    @POST("meeting-proposals")
    suspend fun propose(@Body body: CreateMeetingProposalRequest): MeetingProposalDto

    @POST("meeting-proposals/{id}/{action}")
    suspend fun respond(@Path("id") id: String, @Path("action") action: String): MeetingProposalDto
}

data class FreeSlotDto(val startsAt: String, val endsAt: String, val sharedBreak: Boolean)

data class SlotSuggestionsDto(val slots: List<FreeSlotDto>?, val suggested: FreeSlotDto?)

data class CreateMeetingProposalRequest(
    val chatRoomId: String,
    val meetingPointId: String,
    val startsAt: String,
    val endsAt: String,
)

data class MeetingProposalDto(
    val id: String,
    val proposerId: String,
    val startsAt: String,
    val endsAt: String,
    val status: String,
    val createdAt: String?,
    val meetingPoint: MeetingPointDto?,
)

interface MeetingProposalRemoteDataSource {
    suspend fun suggestions(chatRoomId: String): SlotSuggestionsDto
    suspend fun proposals(chatRoomId: String): List<MeetingProposalDto>
    suspend fun propose(request: CreateMeetingProposalRequest): MeetingProposalDto
    suspend fun respond(proposalId: String, accept: Boolean): MeetingProposalDto
}

class RetrofitMeetingProposalRemoteDataSource(private val api: MeetingProposalApi) : MeetingProposalRemoteDataSource {
    override suspend fun suggestions(chatRoomId: String) = api.suggestions(chatRoomId)
    override suspend fun proposals(chatRoomId: String) = api.proposals(chatRoomId)
    override suspend fun propose(request: CreateMeetingProposalRequest) = api.propose(request)
    override suspend fun respond(proposalId: String, accept: Boolean) =
        api.respond(proposalId, if (accept) "accept" else "decline")
}
