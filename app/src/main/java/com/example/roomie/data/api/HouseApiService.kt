package com.example.roomie.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.LocalDate

interface HouseApiService {

    /** Creates a house with you as owner and invites [CreateHouseBody.inviteNetids]. */
    @POST("houses")
    suspend fun createHouse(@Body body: CreateHouseBody): HouseResponse

    @GET("houses/{houseId}")
    suspend fun getHouse(@Path("houseId") houseId: Int): HouseResponse

    /** Any member can edit house settings. */
    @PATCH("houses/{houseId}")
    suspend fun updateHouse(@Path("houseId") houseId: Int, @Body body: UpdateHouseBody): HouseResponse

    /** Skips people already in the house. */
    @POST("houses/{houseId}/invites")
    suspend fun invite(@Path("houseId") houseId: Int, @Body body: InviteBody): HouseResponse

    @DELETE("houses/{houseId}/invites/{inviteId}")
    suspend fun cancelInvite(@Path("houseId") houseId: Int, @Path("inviteId") inviteId: Int)

    /** Owner only. The old code stops working. */
    @POST("houses/{houseId}/invite-code")
    suspend fun regenerateInviteCode(@Path("houseId") houseId: Int): HouseResponse

    @POST("houses/join")
    suspend fun joinHouse(@Body body: JoinHouseBody): HouseResponse

    @POST("invites/{inviteId}/accept")
    suspend fun acceptInvite(@Path("inviteId") inviteId: Int): HouseResponse

    @POST("invites/{inviteId}/decline")
    suspend fun declineInvite(@Path("inviteId") inviteId: Int)

    /** Also removes you from rotations, releases your shopping claims and passes on ownership. */
    @DELETE("houses/{houseId}/members/me")
    suspend fun leaveHouse(@Path("houseId") houseId: Int)

    /** Owner only, with the same cleanup as leaving. */
    @DELETE("houses/{houseId}/members/{userId}")
    suspend fun removeMember(@Path("houseId") houseId: Int, @Path("userId") userId: Int)
}

data class HouseResponse(val house: HouseDto)

data class CreateHouseBody(
    val name: String,
    val unit: String? = null,
    val leaseEndsOn: LocalDate? = null,
    /** IANA name. The server defaults to America/New_York. */
    val timezone: String? = null,
    val inviteNetids: List<String>? = null,
)

data class UpdateHouseBody(
    val name: String? = null,
    val unit: String? = null,
    val leaseEndsOn: LocalDate? = null,
    val timezone: String? = null,
)

data class InviteBody(val netids: List<String>)

/** Not case-sensitive. */
data class JoinHouseBody(val inviteCode: String)
