package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Roommate
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.NotePink
import com.example.roomie.ui.theme.NotePurple
import com.example.roomie.ui.theme.NoteYellow
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary

/**
 * Multi-select roommate picker ("People Assigned", "People to Rotate"). The trigger shows the
 * picked people as tags; the panel has an Everyone shortcut and a Done Selecting button.
 */
@Composable
fun PeoplePicker(
    members: List<Roommate>,
    selectedIds: List<String>,
    onToggle: (id: String, selected: Boolean) -> Unit,
    onToggleEveryone: (selected: Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = members.filter { it.id in selectedIds }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassDropdownTrigger(open, { open = !open }, contentDescription, startPadding = 8.dp) {
            if (selected.isEmpty()) {
                Text(
                    stringResource(R.string.people_picker_select),
                    style = RoomieType.choreMeta,
                    modifier = Modifier.padding(start = 6.dp),
                )
            } else {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    selected.forEach { PersonTag(it) }
                }
            }
        }
        if (open) {
            DropdownOptions {
                val everyone = selected.size == members.size
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .toggleable(value = everyone, role = Role.Checkbox, onValueChange = onToggleEveryone)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.people_picker_everyone),
                        style = RoomieType.onboardingLabel.copy(color = TextPrimary),
                        modifier = Modifier.weight(1f),
                    )
                    SoftCheckbox(everyone)
                }
                members.forEach { m ->
                    val checked = m.id in selectedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .toggleable(value = checked, role = Role.Checkbox) { onToggle(m.id, it) }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BuddyAvatar(m.buddy, 22.dp)
                        Text(
                            m.firstName,
                            style = RoomieType.choreMeta.copy(color = TextPrimary),
                            modifier = Modifier.weight(1f),
                        )
                        SoftCheckbox(checked)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(shape)
                        .background(GlassFaint)
                        .border(1.dp, Tan.copy(alpha = 0.5f), shape)
                        .clickable(role = Role.Button) { open = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.people_picker_done), style = RoomieType.smallLink)
                }
            }
        }
    }
}

/** "Roomie / Checkbox": 18dp, soft green with a check when ticked. Visual only. */
@Composable
private fun SoftCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .size(18.dp)
            .clip(shape)
            .background(if (checked) NoteGreen else CorkInner)
            .border(if (checked) 1.dp else 1.2.dp, if (checked) SuccessText else Tan, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painterResource(R.drawable.ic_check_green),
                null,
                Modifier.size(9.75.dp),
                tint = Color.Unspecified,
            )
        }
    }
}

/** Fill behind a roommate's person tag, matching their avatar's background. */
private fun Buddy.tagColor(): Color = when (this) {
    Buddy.Mug -> PeachPill
    Buddy.Sock -> NoteYellow
    Buddy.Plant -> NotePurple
    Buddy.Toaster -> NoteGreen
    Buddy.Teapot -> NotePink
}

/** "Roomie / Person tag": avatar and first name on the roommate's colour. */
@Composable
private fun PersonTag(roommate: Roommate, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier
            .clip(shape)
            .background(roommate.buddy.tagColor())
            .border(1.dp, Tan.copy(alpha = 0.65f), shape)
            .padding(start = 4.dp, end = 7.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyAvatar(roommate.buddy, 18.dp)
        Text(roommate.firstName, style = RoomieType.rowMeta.copy(color = TextPrimary, fontWeight = FontWeight.Medium))
    }
}

@Preview
@Composable
private fun PeoplePickerPreview() = RoomiePreview(padding = 16.dp) {
    val members = FakeRoomieRepository.demoState().house!!.members
    PeoplePicker(members, members.take(2).map { it.id }, { _, _ -> }, {}, "Choose who does this chore")
}
