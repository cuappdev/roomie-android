package com.example.roomie.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One card on Manage Rotations. */
data class RotationSummary(
    val id: String,
    val title: String,
    /** First names, in rotation order. */
    val memberNames: List<String>,
    val peoplePerWeek: Int,
    /** Who has the next turn, and when it's due. */
    val nextNames: List<String>,
    val nextDue: LocalDate,
)

data class ManageRotationsUiState(val rotations: List<RotationSummary>)

@HiltViewModel
class ManageRotationsViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {
    val ui: StateFlow<ManageRotationsUiState> = repo.state
        .map { manageRotationsUiState(it, repo.today()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            manageRotationsUiState(repo.state.value, repo.today()),
        )
}

internal fun manageRotationsUiState(state: RoomieState, today: LocalDate) = ManageRotationsUiState(
    state.rotations.map { rotation ->
        val (nextWeek, nextIds) = ChoreRules.nextTurn(rotation, today)
        val plan = rotation.planFor(nextWeek) ?: rotation.plans.last()
        RotationSummary(
            id = rotation.id,
            title = rotation.title,
            memberNames = plan.memberIds.mapNotNull { state.member(it)?.firstName },
            peoplePerWeek = plan.peoplePerWeek,
            nextNames = nextIds.mapNotNull { state.member(it)?.firstName },
            nextDue = nextWeek.plusDays(rotation.dueDay.value - 1L),
        )
    },
)
