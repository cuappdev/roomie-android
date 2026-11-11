package com.example.roomie.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDate
import java.time.YearMonth

interface ChoreApiService {

    @GET("houses/{houseId}/chores")
    suspend fun getChores(@Path("houseId") houseId: Int): ChoresResponse

    /** The first person in [CreateChoreBody.rotation] does the first turn. */
    @POST("houses/{houseId}/chores")
    suspend fun createChore(@Path("houseId") houseId: Int, @Body body: CreateChoreBody): ChoreResponse

    @GET("houses/{houseId}/chores/{choreId}")
    suspend fun getChore(@Path("houseId") houseId: Int, @Path("choreId") choreId: Int): ChoreResponse

    /** Finished and overdue turns never change. A new schedule can't start in the past. */
    @PATCH("houses/{houseId}/chores/{choreId}")
    suspend fun updateChore(
        @Path("houseId") houseId: Int,
        @Path("choreId") choreId: Int,
        @Body body: UpdateChoreBody,
    ): ChoreResponse

    /** Finished turns stay in the history and stats. */
    @DELETE("houses/{houseId}/chores/{choreId}")
    suspend fun deleteChore(@Path("houseId") houseId: Int, @Path("choreId") choreId: Int)

    /**
     * Turns due between [from] and [to] (at most 62 days apart), sorted by due date.
     * Home uses `from = to = today, assignee = "me", includeOverdue = true`.
     *
     * @param assignee `"me"` or a user ID.
     * @param includeOverdue Adds unfinished turns from the 30 days before [from].
     */
    @GET("houses/{houseId}/chore-assignments")
    suspend fun getAssignments(
        @Path("houseId") houseId: Int,
        @Query("from") from: LocalDate,
        @Query("to") to: LocalDate,
        @Query("assignee") assignee: String? = null,
        @Query("includeOverdue") includeOverdue: Boolean? = null,
    ): AssignmentsResponse

    /** Anyone in the house can do it, and it counts for whoever did. */
    @POST("houses/{houseId}/chore-assignments/{assignmentId}/complete")
    suspend fun completeAssignment(
        @Path("houseId") houseId: Int,
        @Path("assignmentId") assignmentId: Int,
    ): AssignmentResponse

    @DELETE("houses/{houseId}/chore-assignments/{assignmentId}/complete")
    suspend fun uncompleteAssignment(
        @Path("houseId") houseId: Int,
        @Path("assignmentId") assignmentId: Int,
    ): AssignmentResponse

    /** Trades two upcoming turns of the same chore. You must hold one of them. */
    @POST("houses/{houseId}/chore-assignments/{assignmentId}/swap")
    suspend fun swapAssignment(
        @Path("houseId") houseId: Int,
        @Path("assignmentId") assignmentId: Int,
        @Body body: SwapBody,
    ): AssignmentsResponse

    /** Turns each person finished in [month] (defaults to this month). */
    @GET("houses/{houseId}/chores/stats")
    suspend fun getChoreStats(
        @Path("houseId") houseId: Int,
        @Query("month") month: YearMonth? = null,
    ): ChoreStatsResponse
}

data class ChoresResponse(val chores: List<ChoreDto>)

data class ChoreResponse(val chore: ChoreDto)

data class AssignmentsResponse(val assignments: List<TurnDto>)

data class AssignmentResponse(val assignment: TurnDto)

data class ChoreStatsResponse(val month: String, val members: List<MemberChoreStatsDto>)

data class MemberChoreStatsDto(val user: UserDto, val done: Int)

data class CreateChoreBody(
    val title: String,
    val frequency: ChoreFrequency,
    val startsOn: LocalDate,
    /** User IDs in turn order. */
    val rotation: List<Int>,
    val allowSwaps: Boolean? = null,
    val reminderHour: Int? = null,
)

data class UpdateChoreBody(
    val title: String? = null,
    val frequency: ChoreFrequency? = null,
    val startsOn: LocalDate? = null,
    val rotation: List<Int>? = null,
    val allowSwaps: Boolean? = null,
    val reminderHour: Int? = null,
)

data class SwapBody(val withAssignmentId: Int)
