package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.Chore
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Roommate
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.DoneCardStart
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.monthDayLabel
import com.example.roomie.ui.util.shortLabel
import java.time.LocalDate

/** "Mina", "Mina and Leah", "Mina, Leah and Stef". */
@Composable
private fun List<Roommate>.namesLabel(): String =
    joinNames(map { it.firstName }).ifEmpty { stringResource(R.string.unassigned) }

/** "Chores / Card": title with a completion circle, assignees, due line and the rotation badge. */
@Composable
fun ChoreCard(
    chore: Chore,
    assignees: List<Roommate>,
    today: LocalDate,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val doneDescription = stringResource(R.string.cd_chore_done, chore.title)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                4.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.1f),
                spotColor = ShadowWarm.copy(alpha = 0.1f),
            )
            .clip(shape)
            .background(
                if (chore.done) Brush.horizontalGradient(listOf(DoneCardStart, Surface)) else SolidColor(Surface),
            )
            .border(0.7.dp, if (chore.done) NoteGreen else Border, shape)
            .clickable(onClickLabel = stringResource(R.string.cd_chore_open, chore.title), onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                chore.title,
                style = RoomieType.choreTitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                painterResource(if (chore.done) R.drawable.ic_chore_done else R.drawable.ic_chore_pending),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .toggleable(value = chore.done, role = Role.Checkbox, onValueChange = { onToggle() })
                    .semantics { contentDescription = doneDescription },
                tint = Color.Unspecified,
            )
        }
        if (assignees.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    assignees.forEach { BuddyAvatar(it.buddy, 24.dp) }
                }
                Text(
                    assignees.namesLabel(),
                    style = RoomieType.choreMeta,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
            val overdue = !chore.done && chore.dueDate.isBefore(today)
            Text(
                when {
                    chore.done -> stringResource(R.string.chore_completed_short, chore.completedOn!!.monthDayLabel())
                    chore.dueDate == today -> stringResource(R.string.chore_due_today)
                    overdue -> stringResource(R.string.chore_overdue_short, chore.dueDate.shortLabel())
                    else -> stringResource(R.string.chore_due_short, chore.dueDate.shortLabel())
                },
                style = RoomieType.choreMeta.copy(color = if (overdue) Danger else TextSecondary),
                modifier = Modifier.weight(1f).wrapContentHeight(Alignment.CenterVertically),
            )
            if (chore.rotationId != null) RotationBadge()
        }
    }
}

@Composable
private fun RotationBadge(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(stringResource(R.string.chore_weekly_rotation), style = RoomieType.smallLink)
    }
}

@Preview
@Composable
private fun ChoreCardPreview() = RoomiePreview(padding = 16.dp) {
    val state = FakeRoomieRepository.demoState()
    val today = LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoreRules.choresInWeek(state, ChoreRules.weekStart(today), today).take(3).forEach { chore ->
            ChoreCard(chore, chore.assigneeIds.mapNotNull(state::member), today, onToggle = {}, onClick = {})
        }
    }
}
