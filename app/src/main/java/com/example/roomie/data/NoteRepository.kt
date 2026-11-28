package com.example.roomie.data

import com.example.roomie.data.api.CreateNoteBody
import com.example.roomie.data.api.NoteBoardResponse
import com.example.roomie.data.api.NoteDto
import com.example.roomie.data.api.ResponseFlow
import com.example.roomie.data.api.RetrofitInstance
import com.example.roomie.data.api.RoomieApiResponse
import com.example.roomie.data.api.apiCall
import com.example.roomie.data.api.repositoryScope
import com.example.roomie.data.api.updateNoteBody
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val retrofitInstance: RetrofitInstance) {

    private val scope = repositoryScope()
    private val api get() = retrofitInstance.noteApi

    private val _noteBoard = ResponseFlow<NoteBoardResponse>(scope)

    /** Everything on the board. Refreshed after every change below succeeds. */
    val noteBoard: StateFlow<RoomieApiResponse<NoteBoardResponse>> = _noteBoard.state

    /** Pipelines the board into [noteBoard]. */
    fun fetchNoteBoard(houseId: Int, refresh: Boolean = false) {
        _noteBoard.fetch(refresh) { api.getNotes(houseId) }
    }

    /** Forgets the board, e.g. on sign-out or after leaving the house. */
    fun clear() = _noteBoard.set(RoomieApiResponse.Pending)

    /** The server picks the color. [dueOn] can't be in the past. */
    suspend fun createNote(houseId: Int, body: String, dueOn: LocalDate? = null, pinned: Boolean = false): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.createNote(houseId, CreateNoteBody(body.trim(), dueOn = dueOn, pinned = pinned)).note }

    /** Your own notes only. Sets both fields; a null [dueOn] clears the due date. */
    suspend fun updateNote(houseId: Int, noteId: Int, body: String, dueOn: LocalDate?): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.updateNote(houseId, noteId, updateNoteBody(body.trim(), dueOn)).note }

    suspend fun deleteNote(houseId: Int, noteId: Int): RoomieApiResponse<Unit> =
        changing(houseId) { api.deleteNote(houseId, noteId) }

    suspend fun pinNote(houseId: Int, noteId: Int): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.pinNote(houseId, noteId).note }

    suspend fun unpinNote(houseId: Int, noteId: Int): RoomieApiResponse<NoteDto> =
        changing(houseId) { api.unpinNote(houseId, noteId).note }

    /** Pinning moves notes between sections, so refetch rather than patch. */
    private suspend fun <T> changing(houseId: Int, call: suspend () -> T): RoomieApiResponse<T> =
        apiCall(call).also { it.ifSuccess { fetchNoteBoard(houseId, refresh = true) } }
}
