package com.example.roomie.ui.chores

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.data.Rotation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The edit in progress: who's in the rotation, how many per week, and from which week. */
data class RotationDraft(val people: List<String>, val perWeek: Int, val fromWeek: LocalDate)

data class EditRotationUiState(
    val rotationId: String,
    val title: String,
    val dueDay: DayOfWeek,
    val members: List<Roommate>,
    val draft: RotationDraft,
    val showPerWeek: Boolean,
    val perWeekOptions: List<Int>,
    /** The backend applies edits from the next turn, so there's no week to pick. */
    val showStartWeek: Boolean,
    val weekOptions: List<LocalDate>,
    val preview: List<Pair<LocalDate, List<Roommate>>>,
    val canSave: Boolean,
)

sealed interface EditRotationEvent {
    data class PersonToggled(val id: String, val selected: Boolean) : EditRotationEvent
    data class EveryoneToggled(val selected: Boolean) : EditRotationEvent
    data class PerWeekPicked(val perWeek: Int) : EditRotationEvent
    data class FromWeekPicked(val week: LocalDate) : EditRotationEvent
}

/** Opened with the rotation's id as the `id` navigation argument. */
@HiltViewModel
class EditRotationViewModel @Inject constructor(
    private val repo: RoomieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val rotationId: String = savedStateHandle["id"] ?: ""

    /** Null until the user changes something; until then the draft is the rotation as it stands. */
    private val draft = MutableStateFlow<RotationDraft?>(null)

    /** Null once the rotation is gone. */
    val ui: StateFlow<EditRotationUiState?> = combine(repo.state, draft) { state, draft ->
        editRotationUiState(state, rotationId, draft, repo.today())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        editRotationUiState(repo.state.value, rotationId, null, repo.today()),
    )

    fun onEvent(event: EditRotationEvent) {
        val current = ui.value ?: return
        draft.value = current.draft.reduce(event, current.members.map { it.id })
    }

    fun save() {
        val current = ui.value ?: return
        repo.updateRotation(rotationId, current.draft.people, current.draft.perWeek, current.draft.fromWeek)
    }

    fun delete() = repo.deleteRotation(rotationId)
}

/** The rotation as it stands, from next week (or from its next turn when the backend picks the week). */
internal fun initialDraft(state: RoomieState, rotation: Rotation, today: LocalDate): RotationDraft {
    val nextWeek = ChoreRules.weekStart(today).plusWeeks(1)
    val base = rotation.planFor(nextWeek) ?: rotation.plans.last()
    val fromWeek = if (state.capabilities.rotationStartWeek) nextWeek else rotation.startWeek
    return RotationDraft(base.memberIds, base.peoplePerWeek, fromWeek)
}

internal fun RotationDraft.reduce(event: EditRotationEvent, houseOrder: List<String>): RotationDraft = when (event) {
    is EditRotationEvent.PersonToggled -> withPeople(
        ChoreRules.toggleMember(houseOrder, people, event.id, event.selected),
    )
    is EditRotationEvent.EveryoneToggled -> withPeople(if (event.selected) houseOrder else emptyList())
    is EditRotationEvent.PerWeekPicked -> copy(perWeek = event.perWeek)
    is EditRotationEvent.FromWeekPicked -> copy(fromWeek = event.week)
}

private fun RotationDraft.withPeople(ids: List<String>) =
    copy(people = ids, perWeek = ChoreRules.clampPerWeek(perWeek, ids.size))

internal fun editRotationUiState(
    state: RoomieState,
    rotationId: String,
    draft: RotationDraft?,
    today: LocalDate,
): EditRotationUiState? {
    val house = state.house ?: return null
    val rotation = state.rotations.firstOrNull { it.id == rotationId } ?: return null
    val capabilities = state.capabilities
    val current = draft ?: initialDraft(state, rotation, today)
    val updated = ChoreRules.withPlan(rotation, current.fromWeek, current.people, current.perWeek)
    val nextWeek = ChoreRules.weekStart(today).plusWeeks(1)
    return EditRotationUiState(
        rotationId = rotation.id,
        title = rotation.title,
        dueDay = rotation.dueDay,
        members = house.members,
        draft = current,
        showPerWeek = capabilities.maxPeoplePerWeek > 1,
        perWeekOptions = (1..current.people.size.coerceIn(1, capabilities.maxPeoplePerWeek)).toList(),
        showStartWeek = capabilities.rotationStartWeek,
        weekOptions = (0L..7L).map { nextWeek.plusWeeks(it) },
        preview = if (current.people.isEmpty()) {
            emptyList()
        } else {
            ChoreRules.previewWeeks(updated, current.fromWeek).map { (week, ids) ->
                week to
                    ids.mapNotNull(state::member)
            }
        },
        canSave = current.people.isNotEmpty(),
    )
}
