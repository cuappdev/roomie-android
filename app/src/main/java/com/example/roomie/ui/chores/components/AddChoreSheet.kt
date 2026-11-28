package com.example.roomie.ui.chores.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.chores.AddChoreEvent
import com.example.roomie.ui.chores.AddChoreForm
import com.example.roomie.ui.chores.AddChoreUiState
import com.example.roomie.ui.chores.addChoreUiState
import com.example.roomie.ui.chores.newAddChoreForm
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.fullLabel
import com.example.roomie.ui.util.weekOfLabel
import java.time.LocalDate

/**
 * "Add chore sheet": slides over the Chores page (the nav bar stays visible below it). Assign Once
 * makes a single chore for the chosen people; Rotate Weekly makes a rotation due on the chosen
 * date's weekday, starting from the chosen week. The form lives in [com.example.roomie.ui.chores.ChoresViewModel];
 * this only draws it and reports what the user does.
 */
@Composable
internal fun AddChoreSheet(ui: AddChoreUiState, onEvent: (AddChoreEvent) -> Unit) {
    val form = ui.form
    val dismiss = { onEvent(AddChoreEvent.Dismiss) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = dismiss)

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(TextPrimary.copy(alpha = 0.14f))
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClickLabel = stringResource(R.string.close),
                    onClick = dismiss,
                ),
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
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.chores_add),
                    style = RoomieType.sheetTitle.copy(
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconAction(R.drawable.ic_close_small, stringResource(R.string.close), dismiss, iconSize = 16.dp)
            }
            Column(
                Modifier
                    .weight(1f)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlassSection(stringResource(R.string.add_chore_details)) {
                    ChoreField(stringResource(R.string.add_chore_name)) {
                        RoomieTextField(
                            value = form.title,
                            onValueChange = { onEvent(AddChoreEvent.TitleChanged(it)) },
                            placeholder = stringResource(R.string.add_chore_name_placeholder),
                            height = 38.dp,
                            textStyle = RoomieType.choreMeta.copy(color = TextPrimary),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            contentDescription = stringResource(R.string.cd_add_chore_name),
                        )
                    }
                    ChoreField(stringResource(R.string.add_chore_due_date)) {
                        GlassDropdownTrigger(calendarOpen, {
                            calendarOpen = !calendarOpen
                        }, stringResource(R.string.cd_add_chore_pick_due_date)) {
                            Text(
                                form.due?.fullLabel() ?: stringResource(R.string.add_chore_pick_date),
                                style = RoomieType.choreMeta.copy(
                                    color = if (form.due ==
                                        null
                                    ) {
                                        TextSecondary
                                    } else {
                                        TextPrimary
                                    },
                                ),
                            )
                        }
                        if (calendarOpen) {
                            RoomieInlineCalendar(
                                selected = form.due,
                                today = ui.today,
                                onPick = {
                                    onEvent(AddChoreEvent.DuePicked(it))
                                    calendarOpen = false
                                },
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                GlassSection(stringResource(R.string.add_chore_assignment)) {
                    if (ui.showModeSwitch) {
                        SegmentedControl(
                            listOf(
                                stringResource(R.string.add_chore_assign_once),
                                stringResource(R.string.add_chore_rotate_weekly),
                            ),
                            if (form.rotate) 1 else 0,
                            { onEvent(AddChoreEvent.RotateChanged(it == 1)) },
                            height = 44.dp,
                            corner = 10.dp,
                            innerCorner = 7.dp,
                        )
                    }
                    Text(
                        if (form.rotate) {
                            stringResource(
                                R.string.add_chore_rotate_hint,
                            )
                        } else {
                            stringResource(R.string.add_chore_once_hint)
                        },
                        style = RoomieType.choreMeta,
                    )
                    if (ui.showPerWeek) {
                        ChoreField(stringResource(R.string.rotation_people_per_week)) {
                            ChoiceDropdown(
                                value = form.perWeek,
                                options = ui.perWeekOptions,
                                label = { peopleLabel(it) },
                                onSelect = { onEvent(AddChoreEvent.PerWeekPicked(it)) },
                                placeholder = stringResource(R.string.choose),
                                contentDescription = stringResource(R.string.cd_rotation_people_per_week),
                            )
                        }
                    }
                    ChoreField(
                        if (form.rotate) {
                            stringResource(
                                R.string.rotation_people_to_rotate,
                            )
                        } else {
                            stringResource(R.string.add_chore_people_assigned)
                        },
                    ) {
                        PeoplePicker(
                            members = ui.members,
                            selectedIds = form.people,
                            onToggle = { id, selected -> onEvent(AddChoreEvent.PersonToggled(id, selected)) },
                            onToggleEveryone = { onEvent(AddChoreEvent.EveryoneToggled(it)) },
                            contentDescription = if (form.rotate) {
                                stringResource(
                                    R.string.cd_rotation_people_to_rotate,
                                )
                            } else {
                                stringResource(R.string.cd_add_chore_people_assigned)
                            },
                        )
                    }
                    if (form.rotate) {
                        ChoreField(stringResource(R.string.rotation_starting_week)) {
                            ChoiceDropdown(
                                value = form.startWeek,
                                options = ui.weekOptions,
                                label = { it.weekOfLabel() },
                                onSelect = { onEvent(AddChoreEvent.StartWeekPicked(it)) },
                                placeholder = stringResource(R.string.choose_a_week),
                                contentDescription = stringResource(R.string.cd_rotation_starting_week),
                            )
                        }
                    }
                }
                if (ui.preview.isNotEmpty()) RotationPreview(ui.preview)
                PrimaryButton(
                    stringResource(R.string.add_chore_create),
                    { onEvent(AddChoreEvent.Create) },
                    modifier = Modifier.padding(vertical = 8.dp),
                    enabled = ui.canCreate,
                    height = 44.dp,
                    gradient = true,
                    corner = 10.dp,
                )
            }
        }
    }
}

private fun previewUi(edit: AddChoreForm.() -> AddChoreForm): AddChoreUiState {
    val state = FakeRoomieRepository.demoState()
    val today = LocalDate.now()
    return addChoreUiState(newAddChoreForm(state, today).edit(), state, state.house!!.members, today)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AddChoreSheetEmptyPreview() = RoomiePreview {
    AddChoreSheet(previewUi { this }) {}
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun AddChoreSheetRotationPreview() = RoomiePreview {
    val members = FakeRoomieRepository.demoState().house!!.members.map { it.id }
    AddChoreSheet(
        previewUi {
            copy(
                title = "Take out recycling",
                due = LocalDate.now().plusDays(2),
                rotate = true,
                rotatePeople = members.take(3),
            )
        },
    ) {}
}
