package com.example.roomie.ui.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.Chore
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.DateButton
import com.example.roomie.ui.components.DangerButton
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.FieldLabel
import com.example.roomie.ui.components.PillButton
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieCheckbox
import com.example.roomie.ui.components.RoomieDatePickerDialog
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SecondaryButton
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.choreDayLabel
import com.example.roomie.ui.util.monthDayLabel
import com.example.roomie.ui.util.shortLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate

class ChoresViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun today(): LocalDate = repo.today()
    fun toggleDone(id: String) = repo.toggleChoreDone(id)
    fun reassign(id: String, to: String) = repo.reassignChore(id, to)
    fun addChore(title: String, assignee: String, due: LocalDate, notes: String) = repo.addChore(title, assignee, due, notes)
    fun updateChore(id: String, title: String, assignee: String, due: LocalDate, notes: String) = repo.updateChore(id, title, assignee, due, notes)
    fun deleteChore(id: String) = repo.deleteChore(id)
}

private val DAY_LETTERS = mapOf(
    DayOfWeek.SUNDAY to "S", DayOfWeek.MONDAY to "M", DayOfWeek.TUESDAY to "T", DayOfWeek.WEDNESDAY to "W",
    DayOfWeek.THURSDAY to "Th", DayOfWeek.FRIDAY to "F", DayOfWeek.SATURDAY to "S",
)

@Composable
fun ChoresScreen(onOpenSettings: () -> Unit, onAddChore: () -> Unit, onEditChore: (String) -> Unit, vm: ChoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    val today = vm.today()
    fun who(id: String) = if (id == me) "You" else state.member(id)?.firstName ?: "Someone"

    // Overdue chores stay in Today until someone checks them off.
    val todays = state.chores
        .filter { it.dueDate == today || (!it.done && it.dueDate.isBefore(today)) }
        .sortedWith(compareBy<Chore> { it.done }.thenBy { it.dueDate })
    // Done chores stay visible (struck through) so a mis-tap can be unchecked.
    val upcoming = state.chores.filter { it.dueDate.isAfter(today) }.sortedBy { it.dueDate }.take(3)
    val week = FakeRoomieRepository.weekOf(today)

    Column(Modifier.fillMaxSize().background(Background)) {
        RoomieHeader("Chores") { SettingsAction(onOpenSettings) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Today:")
                if (todays.isEmpty()) Text("Nothing due today.", style = RoomieType.bodySecondary)
                todays.forEach { chore ->
                    val mine = chore.assigneeId == me
                    val overdue = chore.dueDate.isBefore(today)
                    ChoreRow(
                        chore = chore,
                        meta = when {
                            overdue -> "${who(chore.assigneeId)} · ${chore.dueDate.monthDayLabel()}"
                            mine -> "You · tonight"
                            else -> "${who(chore.assigneeId)} · today"
                        },
                        urgent = (mine || overdue) && !chore.done,
                        onToggle = { vm.toggleDone(chore.id) },
                        onOpen = { onEditChore(chore.id) },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Upcoming:")
                if (upcoming.isEmpty()) Text("Nothing coming up.", style = RoomieType.bodySecondary)
                upcoming.forEach { chore ->
                    ChoreRow(
                        chore = chore,
                        meta = "${who(chore.assigneeId)} · ${chore.dueDate.choreDayLabel(today)}",
                        urgent = false,
                        onToggle = { vm.toggleDone(chore.id) },
                        onOpen = { onEditChore(chore.id) },
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Weekly Chores", Modifier.weight(1f))
                    PillButton("Add chore", onAddChore)
                }
                val rows = week.flatMap { day ->
                    val chores = state.chores.filter { it.dueDate == day }
                    if (chores.isEmpty()) listOf(day to null) else chores.map { day to it }
                }
                rows.forEachIndexed { i, (day, chore) ->
                    WeeklyRow(
                        day = day,
                        isToday = day == today,
                        chore = chore,
                        members = house.members,
                        me = me,
                        onReassign = { to -> chore?.let { vm.reassign(it.id, to) } },
                        onOpen = { chore?.let { onEditChore(it.id) } },
                    )
                    if (i < rows.lastIndex) HorizontalDivider(color = Divider, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = RoomieType.sectionTitle, modifier = modifier.semantics { heading() })
}

@Composable
private fun ChoreRow(chore: Chore, meta: String, urgent: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, Divider, shape)
            .padding(start = 5.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomieCheckbox(chore.done, { onToggle() }, "${chore.title} done")
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClickLabel = "Edit ${chore.title}", onClick = onOpen),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                chore.title,
                style = RoomieType.body.copy(fontWeight = FontWeight.Medium, textDecoration = if (chore.done) TextDecoration.LineThrough else null),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta,
                style = RoomieType.caption.copy(fontWeight = FontWeight.SemiBold, color = if (urgent) Danger else TextSecondary),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun WeeklyRow(
    day: LocalDate,
    isToday: Boolean,
    chore: Chore?,
    members: List<Roommate>,
    me: String?,
    onReassign: (String) -> Unit,
    onOpen: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isToday) BrandBrown else PeachPill),
            contentAlignment = Alignment.Center,
        ) {
            Text(DAY_LETTERS.getValue(day.dayOfWeek), style = RoomieType.rowMeta.copy(fontWeight = FontWeight.Medium, color = if (isToday) Surface else com.example.roomie.ui.theme.TextPrimary))
        }
        if (chore == null) {
            Text("Nothing planned", style = RoomieType.bodySecondary, modifier = Modifier.weight(1f))
        } else {
            Text(
                chore.title,
                style = RoomieType.body.copy(textDecoration = if (chore.done) TextDecoration.LineThrough else null),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clickable(role = Role.Button, onClickLabel = "Edit ${chore.title}", onClick = onOpen)
                    .wrapContentHeight(Alignment.CenterVertically),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val assignee = members.firstOrNull { it.id == chore.assigneeId }
            Box {
                Row(
                    Modifier
                        .height(44.dp)
                        .clickable(role = Role.DropdownList, onClickLabel = "Reassign ${chore.title}") { menuOpen = true },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(assignee?.firstName ?: "Unassigned", style = RoomieType.rowTitle)
                    Icon(painterResource(R.drawable.ic_caret_down), null, Modifier.size(width = 10.dp, height = 6.dp), tint = Color.Unspecified)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Surface) {
                    members.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(if (m.id == me) "${m.firstName} (you)" else m.firstName, style = RoomieType.body) },
                            leadingIcon = { BuddyAvatar(m.buddy, 24.dp) },
                            onClick = { onReassign(m.id); menuOpen = false },
                        )
                    }
                }
            }
        }
    }
}

/** Adds a chore, or edits the chore with [choreId] when it is set. */
@Composable
fun ChoreEditorScreen(choreId: String?, onDone: () -> Unit, vm: ChoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    val today = vm.today()
    val existing = choreId?.let { id -> state.chores.firstOrNull { it.id == id } }
    if (choreId != null && existing == null) {
        // Deleted, possibly by a roommate while this was open.
        LaunchedEffect(Unit) { onDone() }
        return
    }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var assignee by rememberSaveable { mutableStateOf(existing?.assigneeId ?: me ?: house.members.first().id) }
    var due by rememberSaveable { mutableStateOf(existing?.dueDate ?: today) }
    var notes by rememberSaveable { mutableStateOf(existing?.notes ?: "") }
    var picking by remember { mutableStateOf(false) }

    fun save() {
        if (existing == null) vm.addChore(title, assignee, due, notes)
        else vm.updateChore(existing.id, title, assignee, due, notes)
    }

    Column(Modifier.fillMaxSize().background(Background)) {
        RoomieHeader(if (existing == null) "New chore" else "Edit chore") { TextAction("Cancel", onDone) }
        Column(
            Modifier
                .weight(1f)
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledField("Chore") {
                    RoomieTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = "e.g. Clean the oven",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        contentDescription = "Chore",
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel("Assign to")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        house.members.forEach { m -> AssigneeOption(m, m.id == assignee) { assignee = m.id } }
                    }
                }
                LabeledField("Due date") {
                    DateButton(due.shortLabel(), { picking = true }, contentDescription = "Pick a due date")
                }
                LabeledField("Notes · optional") {
                    RoomieTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = "Anything they should know?",
                        height = null,
                        minHeight = 84.dp,
                        textStyle = RoomieType.input.copy(lineHeight = 22.4.sp),
                        contentDescription = "Notes",
                    )
                }
            }
            if (existing != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton(
                        if (existing.done) "Mark not done" else "Mark done",
                        { if (title.isNotBlank()) save(); vm.toggleDone(existing.id); onDone() },
                        Modifier.weight(1f),
                    )
                    DangerButton("Delete", { vm.deleteChore(existing.id) }, Modifier.weight(1f))
                }
            }
            PrimaryButton(
                if (existing == null) "Add chore" else "Save changes",
                { save(); onDone() },
                enabled = title.isNotBlank(),
            )
        }
    }

    if (picking) {
        RoomieDatePickerDialog(initial = due, today = today, onPicked = { due = it }, onDismiss = { picking = false })
    }
}

@Composable
private fun AssigneeOption(member: Roommate, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(62.dp)
            .height(82.dp)
            .clip(shape)
            .background(if (selected) PeachPill else Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) BrandBrown else Border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        BuddyAvatar(member.buddy, 34.dp)
        Text(member.firstName, style = RoomieType.rowMeta.copy(color = com.example.roomie.ui.theme.TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium), maxLines = 1)
    }
}
