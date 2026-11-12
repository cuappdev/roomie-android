package com.example.roomie.data

import com.example.roomie.data.api.CreateHouseBody
import com.example.roomie.data.api.HouseDto
import com.example.roomie.data.api.InviteBody
import com.example.roomie.data.api.JoinHouseBody
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.UpdateHouseBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.fetchInto
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.houseApi

    private val _house = MutableStateFlow<RoomieApiResponse<HouseDto>>(RoomieApiResponse.Pending)

    /** Your house. Every call below that returns the house updates this too. */
    val house: StateFlow<RoomieApiResponse<HouseDto>> = _house.asStateFlow()

    /** Pipelines the response into [house]. */
    fun fetchHouse(houseId: Int, refresh: Boolean = false) {
        scope.fetchInto(_house, refresh) { api.getHouse(houseId).house }
    }

    suspend fun createHouse(body: CreateHouseBody): RoomieApiResponse<HouseDto> =
        updatingHouse { api.createHouse(body).house }

    suspend fun updateHouse(houseId: Int, body: UpdateHouseBody): RoomieApiResponse<HouseDto> =
        updatingHouse { api.updateHouse(houseId, body).house }

    suspend fun invite(houseId: Int, netIds: List<String>): RoomieApiResponse<HouseDto> =
        updatingHouse { api.invite(houseId, InviteBody(netIds)).house }

    suspend fun cancelInvite(houseId: Int, inviteId: Int): RoomieApiResponse<Unit> =
        apiCall { api.cancelInvite(houseId, inviteId) }.also { it.ifSuccess { fetchHouse(houseId, refresh = true) } }

    suspend fun regenerateInviteCode(houseId: Int): RoomieApiResponse<HouseDto> =
        updatingHouse { api.regenerateInviteCode(houseId).house }

    suspend fun joinHouse(inviteCode: String): RoomieApiResponse<HouseDto> =
        updatingHouse { api.joinHouse(JoinHouseBody(inviteCode.trim())).house }

    suspend fun acceptInvite(inviteId: Int): RoomieApiResponse<HouseDto> =
        updatingHouse { api.acceptInvite(inviteId).house }

    suspend fun declineInvite(inviteId: Int): RoomieApiResponse<Unit> =
        apiCall { api.declineInvite(inviteId) }

    suspend fun leaveHouse(houseId: Int): RoomieApiResponse<Unit> =
        apiCall { api.leaveHouse(houseId) }.also { it.ifSuccess { _house.value = RoomieApiResponse.Pending } }

    suspend fun removeMember(houseId: Int, userId: Int): RoomieApiResponse<Unit> =
        apiCall { api.removeMember(houseId, userId) }.also { it.ifSuccess { fetchHouse(houseId, refresh = true) } }

    private suspend fun updatingHouse(call: suspend () -> HouseDto): RoomieApiResponse<HouseDto> =
        apiCall(call).also { result -> result.ifSuccess { _house.value = RoomieApiResponse.Success(it) } }
}
