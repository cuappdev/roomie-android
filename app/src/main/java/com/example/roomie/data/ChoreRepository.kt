package com.example.roomie.data

import com.example.roomie.data.api.ChoreDto
import com.example.roomie.data.api.ChoreStatsResponse
import com.example.roomie.data.api.KeyedResponseFlow
import com.example.roomie.data.api.CreateChoreBody
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.SwapBody
import com.example.roomie.data.api.TurnDto
import com.example.roomie.data.api.TurnStatus
import com.example.roomie.data.api.UpdateChoreBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.ResponseFlow
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChoreRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.choreApi

    private val _chores = ResponseFlow<List<ChoreDto>>(scope)

    /** Every chore in the house. */
    val chores: StateFlow<RoomieApiResponse<List<ChoreDto>>> = _chores.state

    private val _myTurnsToday = ResponseFlow<List<TurnDto>>(scope)

    /** Your turns due today, plus any you've left overdue. What Home shows. */
    val myTurnsToday: StateFlow<RoomieApiResponse<List<TurnDto>>> = _myTurnsToday.state

    private val _turns = ResponseFlow<List<TurnDto>>(scope)

    /** Everyone's turns over the range last passed to [fetchTurns], plus overdue ones. */
    val turns: StateFlow<RoomieApiResponse<List<TurnDto>>> = _turns.state

    private val _weeks = KeyedResponseFlow<LocalDate, List<TurnDto>>(scope)

    /**
     * Everyone's turns for each week passed to [fetchWeek] (keyed by its Monday), plus turns left
     * unfinished in the 30 days before it. Weeks stay cached so switching back is instant.
     */
    val weeks: StateFlow<Map<LocalDate, RoomieApiResponse<List<TurnDto>>>> = _weeks.state

    private val _stats = ResponseFlow<ChoreStatsResponse>(scope)

    /** How many turns each member finished in the month last passed to [fetchStats]. */
    val stats: StateFlow<RoomieApiResponse<ChoreStatsResponse>> = _stats.state

    /** Pipelines the response into [chores]. */
    fun fetchChores(houseId: Int, refresh: Boolean = false) {
        _chores.fetch(refresh) { api.getChores(houseId).chores }
    }

    /** The house and day [myTurnsToday] was last fetched for, so completing a turn can refetch it. */
    @Volatile
    private var myTurnsQuery: Pair<Int, LocalDate>? = null

    /** Pipelines the response into [myTurnsToday]. */
    fun fetchMyTurnsToday(houseId: Int, today: LocalDate, refresh: Boolean = false) {
        myTurnsQuery = houseId to today
        _myTurnsToday.fetch(refresh) {
            api.getAssignments(houseId, today, today, assignee = "me", includeOverdue = true).assignments
        }
    }

    /** Pipelines [from] to [to] (at most 62 days) into [turns]. */
    fun fetchTurns(houseId: Int, from: LocalDate, to: LocalDate, refresh: Boolean = false) {
        _turns.fetch(refresh) {
            api.getAssignments(houseId, from, to, includeOverdue = true).assignments
        }
    }

    /** Pipelines Monday [weekStart] to Sunday into [weeks]. */
    fun fetchWeek(houseId: Int, weekStart: LocalDate, refresh: Boolean = false) {
        _weeks.fetch(weekStart, refresh) {
            api.getAssignments(houseId, weekStart, weekStart.plusDays(6), includeOverdue = true).assignments
        }
    }

    /** Refetches every week in [weeks], e.g. after a rotation change reschedules turns. */
    fun refreshWeeks(houseId: Int) {
        _weeks.keys.forEach { fetchWeek(houseId, it, refresh = true) }
    }

    /** Forgets everything, e.g. on sign-out or after leaving the house. */
    fun clear() {
        _chores.set(RoomieApiResponse.Pending)
        _myTurnsToday.set(RoomieApiResponse.Pending)
        _turns.set(RoomieApiResponse.Pending)
        _stats.set(RoomieApiResponse.Pending)
        _weeks.clear()
        myTurnsQuery = null
    }

    /** Pipelines the response into [stats]. Leave [month] null for this month. */
    fun fetchStats(houseId: Int, month: YearMonth? = null, refresh: Boolean = false) {
        _stats.fetch(refresh) { api.getChoreStats(houseId, month) }
    }

    suspend fun getChore(houseId: Int, choreId: Int): RoomieApiResponse<ChoreDto> =
        apiCall { api.getChore(houseId, choreId).chore }

    suspend fun createChore(houseId: Int, body: CreateChoreBody): RoomieApiResponse<ChoreDto> =
        changingChores(houseId) { api.createChore(houseId, body).chore }

    suspend fun updateChore(houseId: Int, choreId: Int, body: UpdateChoreBody): RoomieApiResponse<ChoreDto> =
        changingChores(houseId) { api.updateChore(houseId, choreId, body).chore }

    suspend fun deleteChore(houseId: Int, choreId: Int): RoomieApiResponse<Unit> =
        changingChores(houseId) { api.deleteChore(houseId, choreId) }

    /**
     * A finished overdue turn leaves [myTurnsToday] right away (turns due today stay, shown done).
     * [myTurnsToday] is then refetched in the background to pick up anything else that moved.
     */
    suspend fun completeTurn(houseId: Int, turnId: Int): RoomieApiResponse<TurnDto> =
        apiCall { api.completeAssignment(houseId, turnId).assignment }.also { it.ifSuccess { turn -> changedTurns(houseId, listOf(turn)) } }

    /** Un-finishing an overdue turn puts it back on [myTurnsToday], which the refetch picks up. */
    suspend fun uncompleteTurn(houseId: Int, turnId: Int): RoomieApiResponse<TurnDto> =
        apiCall { api.uncompleteAssignment(houseId, turnId).assignment }.also { it.ifSuccess { turn -> changedTurns(houseId, listOf(turn)) } }

    /** Swapping can move a turn on or off your list, so [myTurnsToday] is refetched. */
    suspend fun swapTurns(houseId: Int, turnId: Int, withTurnId: Int, today: LocalDate): RoomieApiResponse<List<TurnDto>> =
        apiCall { api.swapAssignment(houseId, turnId, SwapBody(withTurnId)).assignments }.also {
            it.ifSuccess { turns ->
                replaceTurns(turns)
                fetchMyTurnsToday(houseId, today, refresh = true)
            }
        }

    /** A chore change can rewrite its upcoming turns, so refetch the chores and the cached weeks. */
    private suspend fun <T> changingChores(houseId: Int, call: suspend () -> T): RoomieApiResponse<T> =
        apiCall(call).also {
            it.ifSuccess {
                fetchChores(houseId, refresh = true)
                refreshWeeks(houseId)
            }
        }

    /** Patches the cached lists, then refetches [myTurnsToday] if it's for the same house. */
    private fun changedTurns(houseId: Int, changed: List<TurnDto>) {
        replaceTurns(changed)
        myTurnsQuery?.let { (queriedHouse, day) ->
            if (queriedHouse == houseId) fetchMyTurnsToday(houseId, day, refresh = true)
        }
    }

    /**
     * Swaps the changed turns into the cached lists that already have them. [myTurnsToday] also
     * drops turns that are done but were due before its day, as the server would.
     * Each list is patched under its own lock, so concurrent mutations can't lose each other's changes.
     */
    private fun replaceTurns(changed: List<TurnDto>) {
        val byId = changed.associateBy { it.id }
        _turns.update { response -> response.map { turns -> turns.map { byId[it.id] ?: it } } }
        _weeks.updateAll { response -> response.map { turns -> turns.map { byId[it.id] ?: it } } }
        val day = myTurnsQuery?.second
        _myTurnsToday.update { response ->
            response.map { turns ->
                turns.map { byId[it.id] ?: it }
                    .filterNot { day != null && it.status == TurnStatus.DONE && it.dueOn.isBefore(day) }
            }
        }
    }
}
