package com.example.roomie.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface UserApiService {

    /** Signs in, creating the account the first time. Call it right after Google sign-in. */
    @POST("auth/login")
    suspend fun login(): LoginResponse

    /** The user, their house and pending invites. Use it on app launch. */
    @GET("users/me")
    suspend fun getMe(): MeResponse

    @PATCH("users/me")
    suspend fun updateMe(@Body body: UpdateMeBody): MeResponse

    /** Checks the server and its database are up. Needs no token. */
    @GET("health")
    suspend fun health(): HealthResponse
}

/**
 * Open Home if there's a [house], a "Join this house?" prompt if there are [pendingInvites],
 * otherwise onboarding.
 */
data class LoginResponse(
    val isNewUser: Boolean,
    val user: SelfDto,
    val house: HouseDto?,
    val pendingInvites: List<InviteDto>,
)

data class MeResponse(
    val user: SelfDto,
    val house: HouseSummaryDto?,
    val pendingInvites: List<InviteDto>,
)

data class UpdateMeBody(
    val name: String? = null,
    val major: String? = null,
)

data class HealthResponse(val status: String, val database: String)
