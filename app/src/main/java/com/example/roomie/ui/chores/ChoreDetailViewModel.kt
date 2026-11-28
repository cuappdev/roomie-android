package com.example.roomie.ui.chores

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.Chore
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ChoreDetailUiState(
    val chore: Chore,
    val assignees: List<Roommate>,
    val meId: String?,
    val isRotation: Boolean,
    /**
     * "This week's assignment stays with … until completed": an unfinished rotation turn due by the
     * end of this week.
     */
    val showStaysWithNote: Boolean,
)

/** Opened with the chore's id as the `id` navigation argument. */
@HiltViewModel
class ChoreDetailViewModel @Inject constructor(
    private val repo: RoomieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val choreId: String = savedStateHandle["id"] ?: ""

    /** Null once the chore is gone, possibly deleted by a roommate while it was open. */
    val ui: StateFlow<ChoreDetailUiState?> = repo.state
        .map { choreDetailUiState(it, choreId, repo.today()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            choreDetailUiState(repo.state.value, choreId, repo.today()),
        )

    fun toggleDone() = repo.toggleChoreDone(choreId)

    fun delete() = repo.deleteChore(choreId)
}

internal fun choreDetailUiState(state: RoomieState, choreId: String, today: LocalDate): ChoreDetailUiState? {
    val chore = ChoreRules.choreById(state, choreId) ?: return null
    val isRotation = chore.rotationId != null
    val endOfThisWeek = ChoreRules.weekStart(today).plusDays(6)
    return ChoreDetailUiState(
        chore = chore,
        assignees = chore.assigneeIds.mapNotNull(state::member),
        meId = state.session?.userId,
        isRotation = isRotation,
        showStaysWithNote = isRotation && !chore.done && !chore.dueDate.isAfter(endOfThisWeek),
    )
}
