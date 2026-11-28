package com.example.roomie.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.NoteRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.ui.chores.ChoreItem
import com.example.roomie.ui.notes.NoteItem
import com.example.roomie.ui.notes.OpenNote
import com.example.roomie.ui.notes.noteItem
import com.example.roomie.ui.notes.openNote
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

enum class Greeting { Morning, Afternoon, Evening }

data class HomeUiState(
    val today: LocalDate,
    val greeting: Greeting,
    val members: List<Roommate>,
    /** Dated notes coming up soonest. */
    val comingUp: List<NoteItem>,
    /** The notes in the Home board's spots; [moreNotes] more sit behind a "+N more" stack. */
    val boardPreview: List<NoteItem>,
    val moreNotes: Int,
    /** This week's chores. */
    val tasks: List<ChoreItem>,
    val openNote: OpenNote?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {

    private val openNoteId = MutableStateFlow<String?>(null)

    /** Null while there's no house to show. */
    val ui: StateFlow<HomeUiState?> = combine(repo.state, openNoteId) { state, openId ->
        homeUiState(state, openId, repo.today(), repo.now())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        homeUiState(repo.state.value, null, repo.today(), repo.now()),
    )

    init {
        // Home lists this week's chores. Re-checking the week on every update loads the new week after Sunday night.
        repo.state
            .map { (it.house != null) to ChoreRules.weekStart(repo.today()) }
            .distinctUntilChanged()
            .onEach { (hasHouse, week) -> if (hasHouse) repo.showWeek(week) }
            .launchIn(viewModelScope)
    }

    fun openNote(id: String) {
        openNoteId.value = id
    }

    fun closeNote() {
        openNoteId.value = null
    }

    fun togglePin(id: String) = repo.togglePin(id)

    fun deleteNote(id: String) {
        repo.deleteNote(id)
        closeNote()
    }

    fun toggleChoreDone(id: String) = repo.toggleChoreDone(id)
}

internal fun greetingFor(now: LocalDateTime) = when (now.hour) {
    in 5..11 -> Greeting.Morning
    in 12..16 -> Greeting.Afternoon
    else -> Greeting.Evening
}

internal fun homeUiState(state: RoomieState, openNoteId: String?, today: LocalDate, now: LocalDateTime): HomeUiState? {
    val house = state.house ?: return null
    val (preview, more) = NoteRules.homePreview(state.notes, today)
    return HomeUiState(
        today = today,
        greeting = greetingFor(now),
        members = house.members,
        comingUp = NoteRules.comingUp(state.notes, today).map { noteItem(state, it, today) },
        boardPreview = preview.map { noteItem(state, it, today) },
        moreNotes = more,
        tasks = ChoreRules.choresInWeek(state, ChoreRules.weekStart(today), today)
            .map { ChoreItem(it, it.assigneeIds.mapNotNull(state::member)) },
        openNote = openNote(state, openNoteId, today),
    )
}
