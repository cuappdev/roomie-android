package com.example.roomie.data

import com.example.roomie.data.api.DevTokenProvider
import com.example.roomie.data.api.LoginResponse
import com.example.roomie.data.api.MeResponse
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.UpdateMeBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.fetchInto
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val retrofitInstance: RetrofitInstance,
    private val devTokenProvider: DevTokenProvider,
) {
    private val scope = repositoryScope()

    private val _me = MutableStateFlow<RoomieApiResponse<MeResponse>>(RoomieApiResponse.Pending)

    /** You, your house, and invites you've received. */
    val me: StateFlow<RoomieApiResponse<MeResponse>> = _me.asStateFlow()

    /** Signs in to the local backend or mock server as [netId], with no Firebase involved. */
    suspend fun devLogin(netId: String): RoomieApiResponse<LoginResponse> {
        devTokenProvider.netId = netId.trim().lowercase()
        return login()
    }

    /** Call right after sign-in, once the [com.example.roomie.data.api.TokenProvider] has a token. */
    suspend fun login(): RoomieApiResponse<LoginResponse> = apiCall { retrofitInstance.userApi.login() }

    /** Pipelines the response into [me]. */
    fun fetchMe(refresh: Boolean = false) {
        scope.fetchInto(_me, refresh) { retrofitInstance.userApi.getMe() }
    }

    suspend fun updateMe(name: String? = null, major: String? = null): RoomieApiResponse<MeResponse> =
        apiCall { retrofitInstance.userApi.updateMe(UpdateMeBody(name, major)) }.also { result ->
            result.ifSuccess { _me.value = RoomieApiResponse.Success(it) }
        }

    fun signOut() {
        devTokenProvider.netId = null
        _me.value = RoomieApiResponse.Pending
    }
}
