package com.example.roomie.ui.chores

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.chores.components.BackLink
import com.example.roomie.ui.chores.components.BrownHeaderCard
import com.example.roomie.ui.chores.components.ChoiceDropdown
import com.example.roomie.ui.chores.components.ChoreField
import com.example.roomie.ui.chores.components.GlassSection
import com.example.roomie.ui.chores.components.PeoplePicker
import com.example.roomie.ui.chores.components.RotationPreview
import com.example.roomie.ui.chores.components.SubPage
import com.example.roomie.ui.chores.components.peopleLabel
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.util.weekOfLabel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** "Edit rotation": change people and pace from a future week, with a preview of the next turns. */
@Composable
fun EditRotationScreen(onBack: () -> Unit, vm: EditRotationViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val current = ui
    if (current == null) {
        // Deleted, here or by a roommate.
        LaunchedEffect(Unit) { onBack() }
        return
    }
    EditRotationContent(
        ui = current,
        onBack = onBack,
        onEvent = vm::onEvent,
        onSave = {
            vm.save()
            onBack()
        },
        // The page closes itself once the rotation is gone (see above).
        onDelete = vm::delete,
    )
}

@Composable
private fun EditRotationContent(
    ui: EditRotationUiState,
    onBack: () -> Unit = {},
    onEvent: (EditRotationEvent) -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val draft = ui.draft
    val dueDay = ui.dueDay.getDisplayName(TextStyle.FULL, Locale.US)

    SubPage(spacing = 12) {
        BackLink(stringResource(R.string.back), onBack)
        BrownHeaderCard(ui.title, stringResource(R.string.edit_rotation_subtitle, dueDay))
        GlassSection(stringResource(R.string.edit_rotation_people_schedule)) {
            if (ui.showPerWeek) {
                ChoreField(stringResource(R.string.rotation_people_per_week)) {
                    ChoiceDropdown(
                        value = draft.perWeek,
                        options = ui.perWeekOptions,
                        label = { peopleLabel(it) },
                        onSelect = { onEvent(EditRotationEvent.PerWeekPicked(it)) },
                        placeholder = stringResource(R.string.choose),
                        contentDescription = stringResource(R.string.cd_rotation_people_per_week),
                    )
                }
            }
            ChoreField(stringResource(R.string.rotation_people_to_rotate)) {
                PeoplePicker(
                    members = ui.members,
                    selectedIds = draft.people,
                    onToggle = { id, selected -> onEvent(EditRotationEvent.PersonToggled(id, selected)) },
                    onToggleEveryone = { onEvent(EditRotationEvent.EveryoneToggled(it)) },
                    contentDescription = stringResource(R.string.cd_rotation_people_to_rotate),
                )
            }
            if (ui.showStartWeek) {
                ChoreField(stringResource(R.string.rotation_starting_week)) {
                    ChoiceDropdown(
                        value = draft.fromWeek,
                        options = ui.weekOptions,
                        label = { it.weekOfLabel() },
                        onSelect = { onEvent(EditRotationEvent.FromWeekPicked(it)) },
                        placeholder = stringResource(R.string.choose_a_week),
                        contentDescription = stringResource(R.string.cd_rotation_starting_week),
                    )
                }
            }
        }
        if (ui.preview.isNotEmpty()) RotationPreview(ui.preview)
        PrimaryButton(
            stringResource(R.string.edit_rotation_save),
            onSave,
            enabled = ui.canSave,
            height = 44.dp,
            gradient = true,
            corner = 10.dp,
        )
        Text(
            if (ui.showStartWeek) {
                stringResource(
                    R.string.edit_rotation_applies_future,
                )
            } else {
                stringResource(R.string.edit_rotation_applies_next_turn)
            },
            style = RoomieType.choreMeta,
        )
        TextAction(
            stringResource(R.string.edit_rotation_delete),
            onDelete,
            Modifier.align(Alignment.CenterHorizontally),
            style = RoomieType.linkButton.copy(color = Danger),
        )
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun EditRotationPreview() = RoomiePreview {
    val state = FakeRoomieRepository.demoState()
    EditRotationContent(editRotationUiState(state, state.rotations.first().id, null, LocalDate.now())!!)
}
