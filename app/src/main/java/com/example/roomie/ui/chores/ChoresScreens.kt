package com.example.roomie.ui.chores

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.data.Rotation
import com.example.roomie.data.RotationPlan
import com.example.roomie.ui.components.BackLink
import com.example.roomie.ui.components.BrownHeaderCard
import com.example.roomie.ui.components.ChoiceDropdown
import com.example.roomie.ui.components.ChoreCard
import com.example.roomie.ui.components.ChoreField
import com.example.roomie.ui.components.GlassDropdownTrigger
import com.example.roomie.ui.components.GlassSection
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.PeoplePicker
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RotationPreview
import com.example.roomie.ui.components.SegmentedControl
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.SmallGlassButton
import com.example.roomie.ui.components.TanButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CalendarDayEnd
import com.example.roomie.ui.theme.CalendarDayStart
import com.example.roomie.ui.theme.CalendarSelectedEnd
import com.example.roomie.ui.theme.CalendarSelectedStart
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.fullLabel
import com.example.roomie.ui.util.longLabel
import com.example.roomie.ui.util.monthDayLabel
import com.example.roomie.ui.util.weekOfLabel
import com.example.roomie.ui.util.weekRangeLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ChoresViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun today(): LocalDate = repo.today()
    fun toggleDone(id: String) = repo.toggleChoreDone(id)
    fun addChore(title: String, assigneeIds: List<String>, due: LocalDate) = repo.addChore(title, assigneeIds, due)
    fun addRotation(title: String, dueDay: DayOfWeek, memberIds: List<String>, peoplePerWeek: Int, startWeek: LocalDate) =
        repo.addRotation(title, dueDay, memberIds, peoplePerWeek, startWeek)
    fun deleteChore(id: String) = repo.deleteChore(id)
    fun updateRotation(id: String, memberIds: List<String>, peoplePerWeek: Int, fromWeek: LocalDate) =
        repo.updateRotation(id, memberIds, peoplePerWeek, fromWeek)
    fun deleteRotation(id: String) = repo.deleteRotation(id)
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DayFill = Brush.linearGradient(listOf(CalendarDayStart, CalendarDayEnd))
private val TodayFill = Brush.linearGradient(listOf(CalendarSelectedStart, CalendarSelectedEnd))

private fun peopleLabel(count: Int) = if (count == 1) "1 person" else "$count people"

/** "you" for the signed-in roommate, first names for everyone else. */
private fun assigneeNames(people: List<Roommate>, me: String?): String {
    val names = people.map { if (it.id == me) "you" else it.firstName }
    return if (names.size <= 1) names.firstOrNull() ?: "nobody" else names.dropLast(1).joinToString(", ") + " and " + names.last()
}

@Composable
fun ChoresScreen(
    onOpenSettings: () -> Unit,
    onOpenChore: (String) -> Unit,
    onManageRotations: () -> Unit,
    vm: ChoresViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    val today = vm.today()
    val thisWeek = ChoreRules.weekStart(today)
    var week by rememberSaveable { mutableStateOf(thisWeek) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var mineOnly by rememberSaveable { mutableStateOf(true) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RoomieHeader("Chores", background = Background, titleStyle = RoomieType.greeting, height = 76.dp) { SettingsAction(onOpenSettings) }
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddChoreBar { adding = true }
                WeekPicker(
                    week = week,
                    today = today,
                    expanded = expanded,
                    onToggleExpanded = { expanded = !expanded },
                    onWeekChange = { week = it },
                )
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (week == thisWeek) "This Week" else week.weekRangeLabel(short = true),
                        style = RoomieType.cardTitle.copy(lineHeight = 26.sp),
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    SmallGlassButton("Manage Rotations", onManageRotations)
                }
                SegmentedControl(listOf("My Chores", "All Chores"), if (mineOnly) 0 else 1, { mineOnly = it == 0 })
                val chores = ChoreRules.choresInWeek(state, week, today).filter { !mineOnly || me in it.assigneeIds }
                if (chores.isEmpty()) {
                    Text(
                        if (mineOnly) "No chores assigned to you this week." else "No chores this week.",
                        style = RoomieType.choreMeta,
                    )
                }
                chores.forEach { chore ->
                    ChoreCard(
                        chore = chore,
                        assignees = chore.assigneeIds.mapNotNull(state::member),
                        today = today,
                        onToggle = { vm.toggleDone(chore.id) },
                        onClick = { onOpenChore(chore.id) },
                    )
                }
            }
        }

        if (adding) {
            AddChoreSheet(
                members = house.members,
                me = me,
                today = today,
                onDismiss = { adding = false },
                onAddOnce = { title, people, due -> vm.addChore(title, people, due); adding = false },
                onAddRotation = { title, due, people, perWeek, start ->
                    vm.addRotation(title, due.dayOfWeek, people, perWeek, start)
                    adding = false
                },
            )
        }
    }
}

/** "Add Chore  +": wide tan glass bar under the title. */
@Composable
private fun AddChoreBar(onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(3.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.12f), spotColor = ShadowWarm.copy(alpha = 0.12f))
            .clip(shape)
            .background(Background)
            .background(Brush.horizontalGradient(listOf(CalendarDayStart, CalendarDayEnd)))
            .border(0.8.dp, Color.White.copy(alpha = 0.82f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Add Chore", style = RoomieType.glassButton, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_plus_brown), null, Modifier.size(16.dp), tint = Color.Unspecified)
    }
}

/** "Chores / Week picker": compact week strip that expands to a month; tapping a date picks its week. */
@Composable
private fun WeekPicker(
    week: LocalDate,
    today: LocalDate,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onWeekChange: (LocalDate) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.08f), spotColor = ShadowWarm.copy(alpha = 0.08f))
            .clip(shape)
            .background(Surface)
            .border(0.7.dp, Border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable(role = Role.Button, onClickLabel = if (expanded) "Show one week" else "Show the month", onClick = onToggleExpanded)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                week.weekRangeLabel(),
                style = RoomieType.input.copy(fontWeight = FontWeight.Medium, color = BrandBrown, lineHeight = 20.sp),
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(if (expanded) R.drawable.ic_chevron_up_small else R.drawable.ic_chevron_down_small),
                null,
                Modifier.size(16.dp),
                tint = Color.Unspecified,
            )
        }
        if (expanded) {
            RoomieInlineCalendar(
                selected = today,
                today = today,
                onPick = { onWeekChange(ChoreRules.weekStart(it)); onToggleExpanded() },
                allowPast = true,
                framed = false,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ChoreRules.weekDays(week).forEachIndexed { i, day ->
                    val isToday = day == today
                    Column(
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) TodayFill else DayFill),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Text(DAY_LETTERS[i], style = RoomieType.rowMeta.copy(color = if (isToday) Surface else TextSecondary, lineHeight = 16.sp))
                        Text(
                            day.dayOfMonth.toString(),
                            style = RoomieType.input.copy(fontWeight = FontWeight.Medium, color = if (isToday) Surface else TextPrimary, lineHeight = 20.sp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WeekStep("Previous", R.drawable.ic_chevron_left_small, iconFirst = true) { onWeekChange(week.minusWeeks(1)) }
                Box(Modifier.weight(1f))
                WeekStep("Next week", R.drawable.ic_chevron_right_small, iconFirst = false) { onWeekChange(week.plusWeeks(1)) }
            }
        }
    }
}

@Composable
private fun WeekStep(text: String, icon: Int, iconFirst: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconFirst) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.smallLink.copy(fontWeight = FontWeight.Normal))
        if (!iconFirst) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
    }
}

/**
 * "Add chore sheet": slides over the Chores page (the nav bar stays visible below it). Assign Once
 * makes a single chore for the chosen people; Rotate Weekly makes a rotation due on the chosen
 * date's weekday, starting from the chosen week.
 */
@Composable
private fun AddChoreSheet(
    members: List<Roommate>,
    me: String?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onAddOnce: (String, List<String>, LocalDate) -> Unit,
    onAddRotation: (String, LocalDate, List<String>, Int, LocalDate) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    var rotate by rememberSaveable { mutableStateOf(false) }
    var oncePeople by rememberSaveable { mutableStateOf(listOfNotNull(me)) }
    var rotatePeople by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var perWeek by rememberSaveable { mutableStateOf(1) }
    var startWeek by rememberSaveable { mutableStateOf(ChoreRules.weekStart(today)) }
    val weekOptions = (0L..7L).map { ChoreRules.weekStart(today).plusWeeks(it) }
    val maxPerWeek = rotatePeople.size.coerceIn(1, ChoreRules.MAX_PEOPLE_PER_WEEK)
    val canCreate = title.isNotBlank() && due != null && (if (rotate) rotatePeople.isNotEmpty() else oncePeople.isNotEmpty())

    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TextPrimary.copy(alpha = 0.14f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Close", onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(Background)
                // Swallow taps so they don't reach the scrim behind the sheet.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Divider),
            )
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Add Chore",
                    style = RoomieType.sheetTitle.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconAction(R.drawable.ic_close_small, "Close", onDismiss, iconSize = 16.dp)
            }
            Column(
                Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlassSection("Chore Details") {
                    ChoreField("Chore Name") {
                        RoomieTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "e.g. Take out recycling",
                            height = 38.dp,
                            textStyle = RoomieType.choreMeta.copy(color = TextPrimary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            contentDescription = "Chore name",
                        )
                    }
                    ChoreField("Due Date") {
                        GlassDropdownTrigger(calendarOpen, { calendarOpen = !calendarOpen }, "Pick a due date") {
                            Text(due?.fullLabel() ?: "Pick a date", style = RoomieType.choreMeta.copy(color = if (due == null) TextSecondary else TextPrimary))
                        }
                        if (calendarOpen) {
                            RoomieInlineCalendar(
                                selected = due,
                                today = today,
                                onPick = {
                                    due = it
                                    calendarOpen = false
                                    if (ChoreRules.weekStart(it).isAfter(startWeek)) startWeek = ChoreRules.weekStart(it)
                                },
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                GlassSection("Assignment") {
                    SegmentedControl(
                        listOf("Assign Once", "Rotate Weekly"),
                        if (rotate) 1 else 0,
                        { rotate = it == 1 },
                        height = 44.dp,
                        corner = 10.dp,
                        innerCorner = 7.dp,
                    )
                    Text(
                        if (rotate) "Automatically rotate the assignment each week." else "Choose who does this chore this time.",
                        style = RoomieType.choreMeta,
                    )
                    if (!rotate) {
                        ChoreField("People Assigned") {
                            PeoplePicker(members, oncePeople, { oncePeople = it }, "Choose who does this chore")
                        }
                    } else {
                        ChoreField("People per Week") {
                            ChoiceDropdown(
                                value = perWeek,
                                options = (1..maxPerWeek).toList(),
                                label = ::peopleLabel,
                                onSelect = { perWeek = it },
                                placeholder = "Choose",
                                contentDescription = "People per week",
                            )
                        }
                        ChoreField("People to Rotate") {
                            PeoplePicker(
                                members,
                                rotatePeople,
                                {
                                    rotatePeople = it
                                    perWeek = perWeek.coerceAtMost(it.size.coerceAtLeast(1))
                                },
                                "Choose who's in the rotation",
                            )
                        }
                        ChoreField("Starting Week") {
                            ChoiceDropdown(
                                value = startWeek,
                                options = weekOptions,
                                label = { it.weekOfLabel() },
                                onSelect = { startWeek = it },
                                placeholder = "Choose a week",
                                contentDescription = "Starting week",
                            )
                        }
                    }
                }
                if (rotate && rotatePeople.isNotEmpty()) {
                    val preview = Rotation("preview", title, due?.dayOfWeek ?: DayOfWeek.MONDAY, listOf(RotationPlan(startWeek, rotatePeople, perWeek)))
                    RotationPreview((0L..2L).map { n ->
                        val w = startWeek.plusWeeks(n)
                        w to preview.assigneesFor(w).mapNotNull { id -> members.firstOrNull { it.id == id } }
                    })
                }
                PrimaryButton(
                    "Create Chore",
                    {
                        val date = due ?: return@PrimaryButton
                        if (rotate) onAddRotation(title, date, rotatePeople, perWeek, startWeek) else onAddOnce(title, oncePeople, date)
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                    enabled = canCreate,
                    height = 44.dp,
                    gradient = true,
                    corner = 10.dp,
                )
            }
        }
    }
}

/** "Chore details": who has it, when it's due, and Mark Complete. */
@Composable
fun ChoreDetailScreen(choreId: String, onBack: () -> Unit, onViewRotation: (String) -> Unit, vm: ChoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val me = state.session?.userId
    val chore = ChoreRules.choreById(state, choreId)
    if (chore == null) {
        // Deleted, possibly by a roommate while this was open.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val people = chore.assigneeIds.mapNotNull(state::member)
    val names = assigneeNames(people, me)

    SubPage {
        BackLink("Back to chores", onBack)
        Text(chore.title, style = RoomieType.greeting, modifier = Modifier.semantics { heading() })
        val shape = RoundedCornerShape(10.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(3.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.08f), spotColor = ShadowWarm.copy(alpha = 0.08f))
                .clip(shape)
                .background(Surface)
                .border(0.7.dp, Border, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Assigned to $names", style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
            Text(
                chore.completedOn?.let { "Completed ${it.longLabel()}" } ?: "Due ${chore.dueDate.longLabel()}",
                style = RoomieType.input.copy(color = TextSecondary, lineHeight = 20.sp),
            )
            if (chore.rotationId != null) {
                Text("↻ Weekly rotation", style = RoomieType.smallLink)
                if (!chore.done && !chore.dueDate.isAfter(ChoreRules.weekStart(vm.today()).plusDays(6))) {
                    Text("This week’s assignment stays with $names until completed.", style = RoomieType.choreMeta)
                }
            }
        }
        PrimaryButton(
            if (chore.done) "Mark Not Done" else "Mark Complete",
            {
                vm.toggleDone(chore.id)
                if (!chore.done) onBack()
            },
            height = 44.dp,
            gradient = true,
            corner = 10.dp,
        )
        if (chore.rotationId != null) {
            TanButton("View Rotation", { onViewRotation(chore.rotationId) })
        } else {
            // The page closes itself once the chore is gone (see above).
            TextAction(
                "Delete Chore",
                { vm.deleteChore(chore.id) },
                Modifier.align(Alignment.CenterHorizontally),
                style = RoomieType.linkButton.copy(color = Danger),
            )
        }
    }
}

/** "Manage Rotations": every rotation in the house with who's next. */
@Composable
fun ManageRotationsScreen(onBack: () -> Unit, onOpenRotation: (String) -> Unit, vm: ChoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today = vm.today()

    SubPage(spacing = 12) {
        BackLink("Back to chores", onBack)
        BrownHeaderCard("Manage Rotations", "Automatic weekly assignments")
        if (state.rotations.isEmpty()) {
            Text("No rotations yet. Pick Rotate Weekly when you add a chore.", style = RoomieType.choreMeta)
        }
        state.rotations.forEach { rotation ->
            val (nextWeek, nextIds) = ChoreRules.nextTurn(rotation, today)
            val plan = rotation.planFor(nextWeek) ?: rotation.plans.last()
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.11f), spotColor = ShadowWarm.copy(alpha = 0.11f))
                    .clip(shape)
                    .background(Surface)
                    .border(0.7.dp, Border, shape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(rotation.title, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
                Text(
                    plan.memberIds.mapNotNull { state.member(it)?.firstName }.joinToString(", ") + " · ${peopleLabel(plan.peoplePerWeek)} per week",
                    style = RoomieType.choreMeta,
                )
                Text(
                    "Next: " + nextIds.mapNotNull { state.member(it)?.firstName }.joinToString(", ") + " · ${nextWeek.plusDays(rotation.dueDay.value - 1L).monthDayLabel()}",
                    style = RoomieType.onboardingLabel.copy(color = SuccessText),
                )
                TanButton("View & Edit Rotation", { onOpenRotation(rotation.id) })
            }
        }
    }
}

/** "Edit rotation": change people and pace from a future week, with a preview of the next turns. */
@Composable
fun EditRotationScreen(rotationId: String, onBack: () -> Unit, vm: ChoresViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val rotation = state.rotations.firstOrNull { it.id == rotationId }
    if (rotation == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val today = vm.today()
    val nextWeek = ChoreRules.weekStart(today).plusWeeks(1)
    val base = rotation.planFor(nextWeek) ?: rotation.plans.last()
    var people by rememberSaveable(rotationId) { mutableStateOf(base.memberIds) }
    var perWeek by rememberSaveable(rotationId) { mutableStateOf(base.peoplePerWeek) }
    var fromWeek by rememberSaveable(rotationId) { mutableStateOf(nextWeek) }
    val updated = ChoreRules.withPlan(rotation, fromWeek, people, perWeek)
    val dueDay = rotation.dueDay.getDisplayName(TextStyle.FULL, Locale.US)

    SubPage(spacing = 12) {
        BackLink("Back", onBack)
        BrownHeaderCard(rotation.title, "Weekly rotation · due $dueDay")
        GlassSection("People & Schedule") {
            ChoreField("People per Week") {
                ChoiceDropdown(
                    value = perWeek,
                    options = (1..people.size.coerceIn(1, ChoreRules.MAX_PEOPLE_PER_WEEK)).toList(),
                    label = ::peopleLabel,
                    onSelect = { perWeek = it },
                    placeholder = "Choose",
                    contentDescription = "People per week",
                )
            }
            ChoreField("People to Rotate") {
                PeoplePicker(
                    house.members,
                    people,
                    {
                        people = it
                        perWeek = perWeek.coerceAtMost(it.size.coerceAtLeast(1))
                    },
                    "Choose who's in the rotation",
                )
            }
            ChoreField("Starting Week") {
                ChoiceDropdown(
                    value = fromWeek,
                    options = (0L..7L).map { nextWeek.plusWeeks(it) },
                    label = { it.weekOfLabel() },
                    onSelect = { fromWeek = it },
                    placeholder = "Choose a week",
                    contentDescription = "Starting week",
                )
            }
        }
        if (people.isNotEmpty()) {
            RotationPreview((0L..2L).map { n ->
                val w = fromWeek.plusWeeks(n)
                w to updated.assigneesFor(w).mapNotNull(state::member)
            })
        }
        PrimaryButton(
            "Save Changes",
            {
                vm.updateRotation(rotation.id, people, perWeek, fromWeek)
                onBack()
            },
            enabled = people.isNotEmpty(),
            height = 44.dp,
            gradient = true,
            corner = 10.dp,
        )
        Text("Applies to future weeks only.", style = RoomieType.choreMeta)
        // The page closes itself once the rotation is gone.
        TextAction(
            "Delete Rotation",
            { vm.deleteRotation(rotation.id) },
            Modifier.align(Alignment.CenterHorizontally),
            style = RoomieType.linkButton.copy(color = Danger),
        )
    }
}

/** Scrolling cream page for the chore sub-pages (details, rotations). */
@Composable
private fun SubPage(spacing: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}
