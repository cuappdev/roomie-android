package com.example.roomie.ui.shopping.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.components.DangerButton
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieBottomSheet
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SecondaryButton
import com.example.roomie.ui.shopping.EditItemEvent
import com.example.roomie.ui.shopping.EditItemUiState
import com.example.roomie.ui.shopping.ItemDraft
import com.example.roomie.ui.shopping.Person
import com.example.roomie.ui.shopping.editItemUiState
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.util.agoLabel
import java.time.LocalDateTime

/** "Edit item": rename, change the quantity, add a note, mark it couldn't-find, or delete it. */
@Composable
internal fun EditItemSheet(
    ui: EditItemUiState,
    now: LocalDateTime,
    onEvent: (EditItemEvent) -> Unit,
    onToggleCouldntFind: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val draft = ui.draft
    RoomieBottomSheet(
        title = stringResource(R.string.edit_item_title),
        actionLabel = stringResource(R.string.close),
        onDismiss = onDismiss,
    ) {
        LabeledField(stringResource(R.string.edit_item_item)) {
            RoomieTextField(
                draft.name,
                { onEvent(EditItemEvent.NameChanged(it)) },
                placeholder = stringResource(R.string.edit_item_name_placeholder),
                contentDescription = stringResource(R.string.edit_item_item),
            )
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.edit_item_quantity),
                style = RoomieType.sectionTitle,
                modifier = Modifier.weight(1f),
            )
            StepperButton(
                R.drawable.ic_minus,
                stringResource(R.string.cd_edit_item_decrease),
                enabled =
                draft.quantity > 1,
            ) {
                onEvent(EditItemEvent.Decrease)
            }
            Text(
                "${draft.quantity}",
                style = RoomieType.input.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.widthIn(min = 28.dp),
                textAlign = TextAlign.Center,
            )
            StepperButton(R.drawable.ic_plus_stepper, stringResource(R.string.cd_edit_item_increase), enabled = true) {
                onEvent(EditItemEvent.Increase)
            }
        }
        if (ui.showNote) {
            LabeledField(stringResource(R.string.edit_item_note)) {
                RoomieTextField(
                    draft.note,
                    { onEvent(EditItemEvent.NoteChanged(it)) },
                    placeholder = stringResource(R.string.edit_item_note_placeholder),
                    contentDescription = stringResource(R.string.cd_edit_item_note),
                )
            }
        }
        Text(
            stringResource(
                R.string.edit_item_added_by,
                ui.addedBy.label(capitalized = false),
                ui.addedAt.agoLabel(now),
            ),
            style = RoomieType.caption,
        )
        if (ui.showCouldntFind) {
            SecondaryButton(
                when (ui.couldntFindBy) {
                    null -> stringResource(R.string.edit_item_couldnt_find)
                    Person.Me -> stringResource(R.string.edit_item_found_it)
                    else -> stringResource(R.string.edit_item_clear_couldnt_find)
                },
                onToggleCouldntFind,
                icon = R.drawable.ic_store_18,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DangerButton(stringResource(R.string.delete), onDelete, Modifier.weight(1f), icon = R.drawable.ic_trash)
            PrimaryButton(
                stringResource(R.string.save),
                onSave,
                Modifier.weight(1f),
                enabled = ui.canSave,
                height = 46.dp,
            )
        }
    }
}

@Composable
private fun StepperButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            description,
            Modifier.size(18.dp),
            tint = if (enabled) Color.Unspecified else Border,
        )
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun EditItemSheetPreview() = RoomiePreview {
    val state = FakeRoomieRepository.demoState()
    val item = state.shopping.first { !it.isBought }
    EditItemSheet(
        editItemUiState(state, item, ItemDraft(item.id, item.name, item.quantity, item.note)),
        LocalDateTime.now(),
        onEvent = {},
        onToggleCouldntFind = {},
        onDelete = {},
        onSave = {},
        onDismiss = {},
    )
}
