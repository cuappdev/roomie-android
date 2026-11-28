package com.example.roomie.data.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.time.LocalDate

/**
 * The Note Board. Notes stay on the board for 14 days after posting, or until their due date if
 * that's later; pinned notes stay until someone unpins them. Notes off the board return 404.
 */
interface NoteApiService {

    /** Everything on the board: pinned notes (most recently pinned first), then the rest, newest first. */
    @GET("houses/{houseId}/notes")
    suspend fun getNotes(@Path("houseId") houseId: Int): NoteBoardResponse

    @POST("houses/{houseId}/notes")
    suspend fun createNote(@Path("houseId") houseId: Int, @Body body: CreateNoteBody): NoteResponse

    @GET("houses/{houseId}/notes/{noteId}")
    suspend fun getNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int): NoteResponse

    /** Author only. Build [body] with [updateNoteBody]: clearing the due date needs an explicit null. */
    @PATCH("houses/{houseId}/notes/{noteId}")
    suspend fun updateNote(
        @Path("houseId") houseId: Int,
        @Path("noteId") noteId: Int,
        @Body body: RequestBody,
    ): NoteResponse

    /** The author or the house owner. */
    @DELETE("houses/{houseId}/notes/{noteId}")
    suspend fun deleteNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int)

    /** Any member. Pinning a pinned note moves it to the top. */
    @POST("houses/{houseId}/notes/{noteId}/pin")
    suspend fun pinNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int): NoteResponse

    @DELETE("houses/{houseId}/notes/{noteId}/pin")
    suspend fun unpinNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int): NoteResponse
}

data class NoteBoardResponse(val pinned: List<NoteDto>, val notes: List<NoteDto>)

data class NoteResponse(val note: NoteDto)

data class CreateNoteBody(
    val body: String,
    /** Leave it out and the server picks the color used least recently. */
    val color: NoteColorDto? = null,
    val dueOn: LocalDate? = null,
    /** The "Pin Note" switch. */
    val pinned: Boolean? = null,
    /** The "Notify Others" switch. Accepted, but ignored until push notifications exist. */
    val notify: Boolean? = null,
)

/**
 * A PATCH body for [NoteApiService.updateNote] that sends `body` and `dueOn` together, with
 * `"dueOn": null` when [dueOn] is null so the due date is cleared. (The shared Gson leaves nulls
 * out, which would mean "don't change it".)
 */
fun updateNoteBody(body: String, dueOn: LocalDate?): RequestBody =
    RoomieGson.gsonWithNulls.toJson(mapOf("body" to body, "dueOn" to dueOn?.toString()))
        .toRequestBody("application/json; charset=utf-8".toMediaType())
