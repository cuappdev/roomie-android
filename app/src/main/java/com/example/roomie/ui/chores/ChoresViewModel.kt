package com.example.roomie.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.roomie.data.Chore
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.RoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.data.Rotation
import com.example.roomie.data.RotationPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
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
import kotlinx.coroutines.flow.update

/** A chore with its assignees looked up. */
data class ChoreItem(val chore: Chore, val assignees: List<Roommate>)

data class ChoresUiState(
    val today: LocalDate,
    /** Monday of the week on screen. */
    val week: LocalDate,
    val isThisWeek: Boolean,
    val mineOnly: Boolean,
    val chores: List<ChoreItem>,
    /** The Add Chore sheet, or null while it's closed. */
    val addChore: AddChoreUiState?,
)

/** What's been typed into the Add Chore sheet so far. */
data class AddChoreForm(
    val title: String = "",
    val due: LocalDate? = null,
    /** Rotate Weekly rather than Assign Once. */
    val rotate: Boolean,
    val oncePeople: List<String>,
    val rotatePeople: List<String> = emptyList(),
    val perWeek: Int = 1,
    val startWeek: LocalDate,
) {
    val people: List<String> get() = if (rotate) rotatePeople else oncePeople
}

data class AddChoreUiState(
    val form: AddChoreForm,
    val members: List<Roommate>,
    val today: LocalDate,
    /** The Assign Once / Rotate Weekly switch. Hidden when only rotations are possible. */
    val showModeSwitch: Boolean,
    val showPerWeek: Boolean,
    val perWeekOptions: List<Int>,
    val weekOptions: List<LocalDate>,
    /** Who has the chore for the next few weeks; empty when there's nothing to preview. */
    val preview: List<Pair<LocalDate, List<Roommate>>>,
    val canCreate: Boolean,
)

sealed interface AddChoreEvent {
    data class TitleChanged(val title: String) : AddChoreEvent
    data class DuePicked(val date: LocalDate) : AddChoreEvent
    data class RotateChanged(val rotate: Boolean) : AddChoreEvent
    data class PersonToggled(val id: String, val selected: Boolean) : AddChoreEvent
    data class EveryoneToggled(val selected: Boolean) : AddChoreEvent
    data class PerWeekPicked(val perWeek: Int) : AddChoreEvent
    data class StartWeekPicked(val week: LocalDate) : AddChoreEvent
    data object Create : AddChoreEvent
    data object Dismiss : AddChoreEvent
}

@HiltViewModel
class ChoresViewModel @Inject constructor(private val repo: RoomieRepository) : ViewModel() {

    private val week = MutableStateFlow(ChoreRules.weekStart(repo.today()))
    private val mineOnly = MutableStateFlow(true)
    private val form = MutableStateFlow<AddChoreForm?>(null)

    /** Null while there's no house to show. */
    val ui: StateFlow<ChoresUiState?> = combine(repo.state, week, mineOnly, form) { state, week, mineOnly, form ->
        choresUiState(state, week, mineOnly, form, repo.today())
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        choresUiState(repo.state.value, week.value, mineOnly.value, form.value, repo.today()),
    )

    init {
        // Load each week as it comes on screen, once there's a house to load it for.
        combine(week, repo.state.map { it.house != null }.distinctUntilChanged(), ::Pair)
            .onEach { (week, hasHouse) -> if (hasHouse) repo.showWeek(week) }
            .launchIn(viewModelScope)
    }

    /** Shows the week containing [date]. */
    fun showWeekOf(date: LocalDate) {
        week.value = ChoreRules.weekStart(date)
    }

    fun previousWeek() = week.update { it.minusWeeks(1) }

    fun nextWeek() = week.update { it.plusWeeks(1) }

    fun setMineOnly(mine: Boolean) {
        mineOnly.value = mine
    }

    fun toggleDone(id: String) = repo.toggleChoreDone(id)

    fun openAddChore() {
        val state = repo.state.value
        form.value = newAddChoreForm(state, repo.today())
    }

    fun onAddChoreEvent(event: AddChoreEvent) {
        when (event) {
            AddChoreEvent.Dismiss -> form.value = null
            AddChoreEvent.Create -> create()
            else -> form.update { current ->
                val members = repo.state.value.house?.members.orEmpty().map { it.id }
                current?.reduce(event, members)
            }
        }
    }

    private fun create() {
        val current = form.value ?: return
        val due = current.due ?: return
        if (current.rotate) {
            repo.addRotation(current.title, due.dayOfWeek, current.rotatePeople, current.perWeek, current.startWeek)
        } else {
            repo.addChore(current.title, current.oncePeople, due)
        }
        form.value = null
    }
}

internal fun choresUiState(
    state: RoomieState,
    week: LocalDate,
    mineOnly: Boolean,
    form: AddChoreForm?,
    today: LocalDate,
): ChoresUiState? {
    val house = state.house ?: return null
    val me = state.session?.userId
    val chores = ChoreRules.choresInWeek(state, week, today)
        .filter { !mineOnly || me in it.assigneeIds }
        .map { ChoreItem(it, it.assigneeIds.mapNotNull(state::member)) }
    return ChoresUiState(
        today = today,
        week = week,
        isThisWeek = week == ChoreRules.weekStart(today),
        mineOnly = mineOnly,
        chores = chores,
        addChore = form?.let { addChoreUiState(it, state, house.members, today) },
    )
}

/** A blank form. Without one-time chores the sheet only makes rotations. */
internal fun newAddChoreForm(state: RoomieState, today: LocalDate) = AddChoreForm(
    rotate = !state.capabilities.oneOffChores,
    oncePeople = listOfNotNull(state.session?.userId),
    startWeek = ChoreRules.weekStart(today),
)

internal fun AddChoreForm.reduce(event: AddChoreEvent, houseOrder: List<String>): AddChoreForm = when (event) {
    is AddChoreEvent.TitleChanged -> copy(title = event.title)
    // A due date after the starting week moves the start up to the due date's week.
    is AddChoreEvent.DuePicked -> copy(due = event.date, startWeek = maxOf(startWeek, ChoreRules.weekStart(event.date)))
    is AddChoreEvent.RotateChanged -> copy(rotate = event.rotate)
    is AddChoreEvent.PersonToggled -> withPeople(ChoreRules.toggleMember(houseOrder, people, event.id, event.selected))
    is AddChoreEvent.EveryoneToggled -> withPeople(if (event.selected) houseOrder else emptyList())
    is AddChoreEvent.PerWeekPicked -> copy(perWeek = event.perWeek)
    is AddChoreEvent.StartWeekPicked -> copy(startWeek = event.week)
    AddChoreEvent.Create, AddChoreEvent.Dismiss -> this
}

private fun AddChoreForm.withPeople(ids: List<String>) = if (rotate) {
    copy(
        rotatePeople = ids,
        perWeek = ChoreRules.clampPerWeek(perWeek, ids.size),
    )
} else {
    copy(oncePeople = ids)
}

internal fun addChoreUiState(
    form: AddChoreForm,
    state: RoomieState,
    members: List<Roommate>,
    today: LocalDate,
): AddChoreUiState {
    val capabilities = state.capabilities
    val preview = if (form.rotate && form.rotatePeople.isNotEmpty()) {
        val rotation = Rotation(
            id = "preview",
            title = form.title,
            dueDay = form.due?.dayOfWeek ?: DayOfWeek.MONDAY,
            plans = listOf(RotationPlan(form.startWeek, form.rotatePeople, form.perWeek)),
        )
        ChoreRules.previewWeeks(rotation, form.startWeek).map { (week, ids) -> week to ids.mapNotNull(state::member) }
    } else {
        emptyList()
    }
    return AddChoreUiState(
        form = form,
        members = members,
        today = today,
        showModeSwitch = capabilities.oneOffChores,
        showPerWeek = form.rotate && capabilities.maxPeoplePerWeek > 1,
        perWeekOptions = (1..form.rotatePeople.size.coerceIn(1, capabilities.maxPeoplePerWeek)).toList(),
        weekOptions = (0L..7L).map { ChoreRules.weekStart(today).plusWeeks(it) },
        preview = preview,
        canCreate = form.title.isNotBlank() && form.due != null && form.people.isNotEmpty(),
    )
}
