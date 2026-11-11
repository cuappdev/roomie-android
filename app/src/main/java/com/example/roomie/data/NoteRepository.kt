package com.example.roomie.data

import com.example.roomie.data.api.CreateNoteBody
import com.example.roomie.data.api.NoteApiService
import com.example.roomie.data.api.NoteBoardResponse
import com.example.roomie.data.api.NoteColorDto
import com.example.roomie.data.api.NoteDto
import com.example.roomie.data.api.PinNoteBody
import com.example.roomie.data.api.ReportNoteBody
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.UpdateNoteBody
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.fetchInto
import com.example.roomie.data.api.repositoryScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** The backend doesn't serve the Note Board yet; see [NoteApiService]. */
@Singleton
class NoteRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.noteApi

    private val _noteBoard = MutableStateFlow<RoomieApiResponse<NoteBoardResponse>>(RoomieApiResponse.Pending)

    /** The first page of the board. Refreshed after every change below succeeds. */
    val noteBoard: StateFlow<RoomieApiResponse<NoteBoardResponse>> = _noteBoard.asStateFlow()

    /** Pipelines the first page into [noteBoard]. */
    fun fetchNoteBoard(houseId: Int, refresh: Boolean = false) {
        scope.fetchInto(_noteBoard, refresh) { api.getNotes(houseId, before = null) }
    }

    /** Older notes. Pass the previous page's `nextCursor` as [before]. Doesn't touch [noteBoard]. */
    suspend fun getOlderNotes(houseId: Int, before: Int): RoomieApiResponse<NoteBoardResponse> =
        apiCall { api.getNotes(houseId, before) }

    suspend fun createNote(
        houseId: Int,
        body: String,
        color: NoteColorDto? = null,
        pinnedUntil: Instant? = null,
    ): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.createNote(houseId, CreateNoteBody(body.trim(), color, pinnedUntil)).note }

    suspend fun updateNote(houseId: Int, noteId: Int, body: String? = null, color: NoteColorDto? = null): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.updateNote(houseId, noteId, UpdateNoteBody(body?.trim(), color)).note }

    suspend fun deleteNote(houseId: Int, noteId: Int): RoomieApiResponse<Unit> =
        changing(houseId) { api.deleteNote(houseId, noteId) }

    /** Leave [until] null to pin until someone unpins it. */
    suspend fun pinNote(houseId: Int, noteId: Int, until: Instant? = null): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.pinNote(houseId, noteId, PinNoteBody(until)).note }

    suspend fun unpinNote(houseId: Int, noteId: Int): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.unpinNote(houseId, noteId).note }

    suspend fun reportNote(houseId: Int, noteId: Int, reason: String? = null): RoomieApiResponse<Unit> =
        apiCall { api.reportNote(houseId, noteId, ReportNoteBody(reason)) }

    private suspend fun <T> changing(houseId: Int, call: suspend () -> T): RoomieApiResponse<T> =
        apiCall(call).also { it.ifSuccess { fetchNoteBoard(houseId, refresh = true) } }
}
