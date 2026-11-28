package com.example.roomie.ui.notes.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.NoteRules
import com.example.roomie.ui.components.DropdownField
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.notes.LeaveNoteEvent
import com.example.roomie.ui.notes.LeaveNoteForm
import com.example.roomie.ui.notes.LeaveNoteUiState
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.fullLabel
import java.time.LocalDate

/** "Leave a Note" is a full page in the design; it replaces the board and keeps the nav bar. */
@Composable
internal fun LeaveNoteScreen(ui: LeaveNoteUiState, onEvent: (LeaveNoteEvent) -> Unit) {
    val form = ui.form
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    val sectionLabel = RoomieType.cardTitle.copy(color = TextSecondary)
    val cancel = { onEvent(LeaveNoteEvent.Cancel) }

    BackHandler(onBack = cancel)

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        RoomieHeader(
            stringResource(R.string.leave_note_title),
            background = Background,
            titleStyle = RoomieType.greeting,
            height = 76.dp,
        )
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.leave_note_message), style = sectionLabel)
                RoomieTextField(
                    value = form.text,
                    onValueChange = { onEvent(LeaveNoteEvent.TextChanged(it)) },
                    placeholder = stringResource(R.string.leave_note_placeholder),
                    height = null,
                    minHeight = 72.dp,
                    textStyle = RoomieType.input.copy(lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    contentDescription = stringResource(R.string.leave_note_message),
                )
                if (ui.showCounter) {
                    Text(
                        stringResource(R.string.leave_note_counter, form.text.length, NoteRules.MAX_LENGTH),
                        style = RoomieType.rowMeta,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val dueLabel = stringResource(R.string.leave_note_due_date)
                val optional = stringResource(R.string.leave_note_optional)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            append(dueLabel)
                            withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)) { append(optional) }
                        },
                        style = sectionLabel,
                        modifier = Modifier.weight(1f),
                    )
                    if (form.due != null) {
                        TextAction(
                            stringResource(R.string.leave_note_clear),
                            {
                                onEvent(LeaveNoteEvent.DueCleared)
                                calendarOpen = false
                            },
                            Modifier.height(24.dp),
                            style = RoomieType.smallLink,
                        )
                    }
                }
                DropdownField(
                    text = form.due?.fullLabel(),
                    placeholder = stringResource(R.string.leave_note_select_due_date),
                    expanded = calendarOpen,
                    onClick = { calendarOpen = !calendarOpen },
                    contentDescription = stringResource(R.string.cd_add_chore_pick_due_date),
                )
                if (calendarOpen) {
                    RoomieInlineCalendar(selected = form.due, today = ui.today, onPick = {
                        onEvent(LeaveNoteEvent.DuePicked(it))
                        calendarOpen = false
                    })
                }
                Text(
                    stringResource(R.string.leave_note_stays, NoteRules.OLDER_AFTER_DAYS),
                    style = RoomieType.onboardingNote,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SwitchRow(stringResource(R.string.leave_note_notify), form.notify, onChange = {
                    onEvent(LeaveNoteEvent.NotifyChanged(it))
                })
                SwitchRow(
                    stringResource(R.string.hold_pin),
                    form.pin,
                    onChange = { onEvent(LeaveNoteEvent.PinChanged(it)) },
                    enabled = !ui.pinsFull || form.pin,
                    helper = if (ui.pinsFull &&
                        !form.pin
                    ) {
                        stringResource(R.string.notes_pins_full, NoteRules.MAX_PINS)
                    } else {
                        null
                    },
                )
            }
            PrimaryButton(
                stringResource(R.string.leave_note_post),
                { onEvent(LeaveNoteEvent.Post) },
                enabled = ui.canPost,
                height = 44.dp,
                gradient = true,
                corner = 10.dp,
            )
            GlassButton(stringResource(R.string.cancel), cancel)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    helper: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = RoomieType.input.copy(fontWeight = FontWeight.SemiBold, color = TextSecondary))
            if (helper != null) Text(helper, style = RoomieType.caption)
        }
        RoomieSwitch(checked)
    }
}

/** Visual-only switch; the parent row handles toggling and accessibility. */
@Composable
private fun RoomieSwitch(checked: Boolean) {
    Box(
        Modifier
            .width(46.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) BrandBrown else Divider)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).background(Surface, CircleShape))
    }
}

/** "Roomie / Button / Glass": faint brown fill and outline with small brown text. */
@Composable
private fun GlassButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(BrandBrown.copy(alpha = 0.1f))
            .border(1.dp, BrandBrown.copy(alpha = 0.4f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.smallLink.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun LeaveNoteScreenPreview() = RoomiePreview {
    LeaveNoteScreen(
        LeaveNoteUiState(
            form = LeaveNoteForm(text = "Rent is due Friday", due = LocalDate.now().plusDays(3)),
            today = LocalDate.now(),
            pinsFull = true,
            showCounter = false,
            canPost = true,
        ),
    ) {}
}
