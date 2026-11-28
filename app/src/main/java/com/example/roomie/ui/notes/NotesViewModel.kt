package com.example.roomie.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.NoteRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.ui.notes.components.HoldAction
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** What's been typed into Leave a Note so far. */
data class LeaveNoteForm(
    val text: String = "",
    val due: LocalDate? = null,
    val pin: Boolean = false,
    /** Not wired to anything yet: the app has no notifications. */
    val notify: Boolean = false,
)

data class LeaveNoteUiState(
    val form: LeaveNoteForm,
    val today: LocalDate,
    /** All pins are taken; Pin Note is off unless this note already has one. */
    val pinsFull: Boolean,
    /** The design has no counter; it shows once the limit is close. */
    val showCounter: Boolean,
    val canPost: Boolean,
)

data class NoteBoardUiState(
    val today: LocalDate,
    val showingOlder: Boolean,
    /** The board's notes in order, or the older ones when [showingOlder]. */
    val notes: List<NoteItem>,
    val olderCount: Int,
    val openNote: OpenNote?,
    val editing: NoteItem?,
    val deleting: NoteItem?,
    /** Leave a Note, or null when it's closed. */
    val composing: LeaveNoteUiState?,
)

sealed interface LeaveNoteEvent {
    data class TextChanged(val text: String) : LeaveNoteEvent

    data class DuePicked(val date: LocalDate) : LeaveNoteEvent

    data object DueCleared : LeaveNoteEvent

    data class PinChanged(val pin: Boolean) : LeaveNoteEvent

    data class NotifyChanged(val notify: Boolean) : LeaveNoteEvent

    data object Post : LeaveNoteEvent

    data object Cancel : LeaveNoteEvent
}

/** Which note is open, being edited or being deleted, and the Leave a Note form. */
internal data class BoardSelection(
    val showingOlder: Boolean = false,
    val openId: String? = null,
    val editingId: String? = null,
    val deletingId: String? = null,
    val form: LeaveNoteForm? = null,
)

@HiltViewModel
class NotesViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {

    private val selection = MutableStateFlow(BoardSelection())
    private val _pinsFullEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires when someone tries to pin a note while every pin is taken. */
    val pinsFullEvents: Flow<Unit> = _pinsFullEvents.asSharedFlow()

    val ui: StateFlow<NoteBoardUiState> = combine(repo.state, selection) { state, selection ->
        noteBoardUiState(state, selection, repo.today())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        noteBoardUiState(repo.state.value, selection.value, repo.today()),
    )

    fun showOlder(older: Boolean) = selection.update { it.copy(showingOlder = older) }

    fun openNote(id: String) = selection.update { it.copy(openId = id) }

    fun closeNote() = selection.update { it.copy(openId = null) }

    fun togglePin(id: String) = repo.togglePin(id)

    fun deleteOpenNote() {
        selection.value.openId?.let(repo::deleteNote)
        closeNote()
    }

    /** An action picked from the press-and-hold menu. */
    fun onHoldAction(id: String, action: HoldAction) {
        when (action) {
            HoldAction.Edit -> selection.update { it.copy(editingId = id) }
            HoldAction.Delete -> selection.update { it.copy(deletingId = id) }
            HoldAction.Pin -> {
                val state = repo.state.value
                val today = repo.today()
                val pinned = state.notes.firstOrNull { it.id == id }?.isPinned(today) == true
                if (!pinned && pinsFull(state, today)) _pinsFullEvents.tryEmit(Unit) else repo.togglePin(id)
            }
        }
    }

    fun saveEdit(text: String, due: LocalDate?) {
        selection.value.editingId?.let { repo.updateNote(it, text, due) }
        cancelEdit()
    }

    fun cancelEdit() = selection.update { it.copy(editingId = null) }

    fun confirmDelete() {
        selection.value.deletingId?.let(repo::deleteNote)
        cancelDelete()
    }

    fun cancelDelete() = selection.update { it.copy(deletingId = null) }

    fun startComposing() = selection.update { it.copy(form = LeaveNoteForm()) }

    fun onLeaveNoteEvent(event: LeaveNoteEvent) {
        when (event) {
            LeaveNoteEvent.Cancel -> selection.update { it.copy(form = null) }
            LeaveNoteEvent.Post -> {
                val form = selection.value.form ?: return
                if (form.text.isBlank()) return
                repo.addNote(form.text, form.due, form.pin)
                // A new note goes on the board, so show the board.
                selection.update { it.copy(form = null, showingOlder = false) }
            }
            else -> selection.update { it.copy(form = it.form?.reduce(event)) }
        }
    }
}

internal fun LeaveNoteForm.reduce(event: LeaveNoteEvent): LeaveNoteForm = when (event) {
    is LeaveNoteEvent.TextChanged -> copy(text = event.text.take(NoteRules.MAX_LENGTH))
    is LeaveNoteEvent.DuePicked -> copy(due = event.date)
    LeaveNoteEvent.DueCleared -> copy(due = null)
    is LeaveNoteEvent.PinChanged -> copy(pin = event.pin)
    is LeaveNoteEvent.NotifyChanged -> copy(notify = event.notify)
    LeaveNoteEvent.Post, LeaveNoteEvent.Cancel -> this
}

internal fun noteBoardUiState(state: RoomieState, selection: BoardSelection, today: LocalDate): NoteBoardUiState {
    val older = NoteRules.older(state.notes, today)
    val shown = if (selection.showingOlder) older else NoteRules.boardOrder(state.notes, today)
    fun item(id: String?) = state.notes.firstOrNull { it.id == id }?.let { noteItem(state, it, today) }
    return NoteBoardUiState(
        today = today,
        showingOlder = selection.showingOlder,
        notes = shown.map { noteItem(state, it, today) },
        olderCount = older.size,
        openNote = openNote(state, selection.openId, today),
        editing = item(selection.editingId),
        deleting = item(selection.deletingId),
        composing = selection.form?.let { form ->
            LeaveNoteUiState(
                form = form,
                today = today,
                pinsFull = pinsFull(state, today),
                showCounter = form.text.length > NoteRules.MAX_LENGTH - 20,
                canPost = form.text.isNotBlank(),
            )
        },
    )
}
