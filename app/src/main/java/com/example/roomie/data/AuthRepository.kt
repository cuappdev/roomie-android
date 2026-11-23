package com.example.roomie.data

import com.example.roomie.data.api.DevTokenProvider
import com.example.roomie.data.api.LoginResponse
import com.example.roomie.data.api.MeResponse
import com.example.roomie.data.api.Presence
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.UpdateMeBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.ResponseFlow
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val retrofitInstance: RetrofitInstance,
    private val devTokenProvider: DevTokenProvider,
) {
    private val scope = repositoryScope()

    private val _me = ResponseFlow<MeResponse>(scope)

    /** You, your house, and invites you've received. */
    val me: StateFlow<RoomieApiResponse<MeResponse>> = _me.state

    /** Signs in to the local backend or mock server as [netId], with no Firebase involved. */
    suspend fun devLogin(netId: String): RoomieApiResponse<LoginResponse> {
        devTokenProvider.netId = netId.trim().lowercase()
        return login()
    }

    /** Call right after sign-in, once the [com.example.roomie.data.api.TokenProvider] has a token. */
    suspend fun login(): RoomieApiResponse<LoginResponse> = apiCall { retrofitInstance.userApi.login() }

    /** Pipelines the response into [me]. */
    fun fetchMe(refresh: Boolean = false) {
        _me.fetch(refresh) { retrofitInstance.userApi.getMe() }
    }

    /** Updates [me] too, unless you signed in as someone else while the request was out. */
    suspend fun updateMe(
        name: String? = null,
        major: String? = null,
        presence: Presence? = null,
    ): RoomieApiResponse<MeResponse> {
        val session = devTokenProvider.session
        return apiCall { retrofitInstance.userApi.updateMe(UpdateMeBody(name, major, presence)) }.also { result ->
            result.ifSuccess { if (devTokenProvider.session == session) _me.set(RoomieApiResponse.Success(it)) }
        }
    }

    fun signOut() {
        devTokenProvider.netId = null
        _me.set(RoomieApiResponse.Pending)
    }
}
