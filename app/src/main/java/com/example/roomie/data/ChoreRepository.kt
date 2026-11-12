package com.example.roomie.data

import com.example.roomie.data.api.ChoreDto
import com.example.roomie.data.api.ChoreStatsResponse
import com.example.roomie.data.api.CreateChoreBody
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.SwapBody
import com.example.roomie.data.api.TurnDto
import com.example.roomie.data.api.UpdateChoreBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.fetchInto
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChoreRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.choreApi

    private val _chores = MutableStateFlow<RoomieApiResponse<List<ChoreDto>>>(RoomieApiResponse.Pending)

    /** Every chore in the house. */
    val chores: StateFlow<RoomieApiResponse<List<ChoreDto>>> = _chores.asStateFlow()

    private val _myTurnsToday = MutableStateFlow<RoomieApiResponse<List<TurnDto>>>(RoomieApiResponse.Pending)

    /** Your turns due today, plus any you've left overdue. What Home shows. */
    val myTurnsToday: StateFlow<RoomieApiResponse<List<TurnDto>>> = _myTurnsToday.asStateFlow()

    private val _turns = MutableStateFlow<RoomieApiResponse<List<TurnDto>>>(RoomieApiResponse.Pending)

    /** Everyone's turns over the range last passed to [fetchTurns], plus overdue ones. */
    val turns: StateFlow<RoomieApiResponse<List<TurnDto>>> = _turns.asStateFlow()

    private val _stats = MutableStateFlow<RoomieApiResponse<ChoreStatsResponse>>(RoomieApiResponse.Pending)

    /** How many turns each member finished in the month last passed to [fetchStats]. */
    val stats: StateFlow<RoomieApiResponse<ChoreStatsResponse>> = _stats.asStateFlow()

    /** Pipelines the response into [chores]. */
    fun fetchChores(houseId: Int, refresh: Boolean = false) {
        scope.fetchInto(_chores, refresh) { api.getChores(houseId).chores }
    }

    /** Pipelines the response into [myTurnsToday]. */
    fun fetchMyTurnsToday(houseId: Int, today: LocalDate, refresh: Boolean = false) {
        scope.fetchInto(_myTurnsToday, refresh) {
            api.getAssignments(houseId, today, today, assignee = "me", includeOverdue = true).assignments
        }
    }

    /** Pipelines [from] to [to] (at most 62 days) into [turns]. */
    fun fetchTurns(houseId: Int, from: LocalDate, to: LocalDate, refresh: Boolean = false) {
        scope.fetchInto(_turns, refresh) {
            api.getAssignments(houseId, from, to, includeOverdue = true).assignments
        }
    }

    /** Pipelines the response into [stats]. Leave [month] null for this month. */
    fun fetchStats(houseId: Int, month: YearMonth? = null, refresh: Boolean = false) {
        scope.fetchInto(_stats, refresh) { api.getChoreStats(houseId, month) }
    }

    suspend fun getChore(houseId: Int, choreId: Int): RoomieApiResponse<ChoreDto> =
        apiCall { api.getChore(houseId, choreId).chore }

    suspend fun createChore(houseId: Int, body: CreateChoreBody): RoomieApiResponse<ChoreDto> =
        changingChores(houseId) { api.createChore(houseId, body).chore }

    suspend fun updateChore(houseId: Int, choreId: Int, body: UpdateChoreBody): RoomieApiResponse<ChoreDto> =
        changingChores(houseId) { api.updateChore(houseId, choreId, body).chore }

    suspend fun deleteChore(houseId: Int, choreId: Int): RoomieApiResponse<Unit> =
        changingChores(houseId) { api.deleteChore(houseId, choreId) }

    suspend fun completeTurn(houseId: Int, turnId: Int): RoomieApiResponse<TurnDto> =
        apiCall { api.completeAssignment(houseId, turnId).assignment }.also { it.ifSuccess { turn -> replaceTurns(listOf(turn)) } }

    suspend fun uncompleteTurn(houseId: Int, turnId: Int): RoomieApiResponse<TurnDto> =
        apiCall { api.uncompleteAssignment(houseId, turnId).assignment }.also { it.ifSuccess { turn -> replaceTurns(listOf(turn)) } }

    /** Swapping can move a turn on or off your list, so [myTurnsToday] is refetched. */
    suspend fun swapTurns(houseId: Int, turnId: Int, withTurnId: Int, today: LocalDate): RoomieApiResponse<List<TurnDto>> =
        apiCall { api.swapAssignment(houseId, turnId, SwapBody(withTurnId)).assignments }.also {
            it.ifSuccess { turns ->
                replaceTurns(turns)
                fetchMyTurnsToday(houseId, today, refresh = true)
            }
        }

    /** A chore change can rewrite its upcoming turns, so refetch the chores. Turn lists refresh on their next fetch. */
    private suspend fun <T> changingChores(houseId: Int, call: suspend () -> T): RoomieApiResponse<T> =
        apiCall(call).also { it.ifSuccess { fetchChores(houseId, refresh = true) } }

    /** Swaps the changed turns into the cached lists that already have them. */
    private fun replaceTurns(changed: List<TurnDto>) {
        val byId = changed.associateBy { it.id }
        for (flow in listOf(_turns, _myTurnsToday)) {
            flow.value = flow.value.map { turns -> turns.map { byId[it.id] ?: it } }
        }
    }
}
