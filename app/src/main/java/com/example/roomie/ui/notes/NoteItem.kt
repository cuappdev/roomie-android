package com.example.roomie.ui.notes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.data.NoteRules
import com.example.roomie.data.RoomieState
import java.time.LocalDate

/** A note with what the screens show around it. */
data class NoteItem(
    val note: Note,
    /** Null when the author has left the house. */
    val authorName: String?,
    val pinned: Boolean,
    val isMine: Boolean,
)

/** The note opened in [com.example.roomie.ui.notes.components.NoteDetailDialog]. */
data class OpenNote(val item: NoteItem, val canPin: Boolean)

internal fun noteItem(state: RoomieState, note: Note, today: LocalDate) = NoteItem(
    note = note,
    authorName = state.member(note.authorId)?.firstName,
    pinned = note.isPinned(today),
    isMine = note.authorId == state.session?.userId,
)

internal fun openNote(state: RoomieState, id: String?, today: LocalDate): OpenNote? {
    val note = state.notes.firstOrNull { it.id == id } ?: return null
    return OpenNote(noteItem(state, note, today), canPin = !pinsFull(state, today))
}

internal fun pinsFull(state: RoomieState, today: LocalDate) =
    NoteRules.pinsInUse(state.notes, today) >= NoteRules.MAX_PINS

/** The author's first name, or "Someone". */
@Composable
fun NoteItem.author(): String = authorName ?: stringResource(R.string.someone)
