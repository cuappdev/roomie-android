package com.example.roomie.data.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.Instant

/**
 * The Note Board. This is the proposed contract; the backend and the mock server don't serve
 * it yet, so every call 404s for now.
 */
interface NoteApiService {

    /**
     * Pinned notes, then the rest newest first. Pass the previous page's `nextCursor` as [before]
     * to load older notes. [limit] defaults to 30 (max 100).
     */
    @GET("houses/{houseId}/notes")
    suspend fun getNotes(
        @Path("houseId") houseId: Int,
        @Query("before") before: Int? = null,
        @Query("limit") limit: Int? = null,
    ): NoteBoardResponse

    @POST("houses/{houseId}/notes")
    suspend fun createNote(@Path("houseId") houseId: Int, @Body body: CreateNoteBody): NoteResponse

    @GET("houses/{houseId}/notes/{noteId}")
    suspend fun getNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int): NoteResponse

    /** Your own notes only. */
    @PATCH("houses/{houseId}/notes/{noteId}")
    suspend fun updateNote(
        @Path("houseId") houseId: Int,
        @Path("noteId") noteId: Int,
        @Body body: UpdateNoteBody,
    ): NoteResponse

    /** The author or the house owner. */
    @DELETE("houses/{houseId}/notes/{noteId}")
    suspend fun deleteNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int)

    /** Pinning again replaces the end time. */
    @POST("houses/{houseId}/notes/{noteId}/pin")
    suspend fun pinNote(
        @Path("houseId") houseId: Int,
        @Path("noteId") noteId: Int,
        @Body body: PinNoteBody,
    ): NoteResponse

    @DELETE("houses/{houseId}/notes/{noteId}/pin")
    suspend fun unpinNote(@Path("houseId") houseId: Int, @Path("noteId") noteId: Int): NoteResponse

    /** Hides the note from you. You can't report your own. */
    @POST("houses/{houseId}/notes/{noteId}/report")
    suspend fun reportNote(
        @Path("houseId") houseId: Int,
        @Path("noteId") noteId: Int,
        @Body body: ReportNoteBody,
    )
}

data class NoteBoardResponse(
    /** Only filled on the first page. */
    val pinned: List<NoteDto>,
    val notes: List<NoteDto>,
    /** The `before` value for the next page, or null at the end. */
    val nextCursor: Int?,
)

data class NoteResponse(val note: NoteDto)

data class CreateNoteBody(
    val body: String,
    val color: NoteColorDto? = null,
    val pinnedUntil: Instant? = null,
)

data class UpdateNoteBody(
    val body: String? = null,
    val color: NoteColorDto? = null,
)

/** Leave [until] null to pin until someone unpins it. */
data class PinNoteBody(val until: Instant? = null)

/** Up to 200 characters. */
data class ReportNoteBody(val reason: String? = null)
