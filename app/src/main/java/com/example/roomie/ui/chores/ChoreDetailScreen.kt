package com.example.roomie.ui.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.ChoreRules
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Roommate
import com.example.roomie.ui.chores.components.BackLink
import com.example.roomie.ui.chores.components.SubPage
import com.example.roomie.ui.chores.components.TanButton
import com.example.roomie.ui.chores.components.joinNames
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.longLabel
import java.time.LocalDate

/** "Chore details": who has it, when it's due, and Mark Complete. */
@Composable
fun ChoreDetailScreen(
    onBack: () -> Unit,
    onViewRotation: (String) -> Unit,
    vm: ChoreDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val current = ui
    if (current == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    ChoreDetailContent(
        ui = current,
        onBack = onBack,
        onToggleDone = {
            vm.toggleDone()
            // Completing it is the end of the job; un-completing keeps the page open.
            if (!current.chore.done) onBack()
        },
        onViewRotation = { current.chore.rotationId?.let(onViewRotation) },
        // The page closes itself once the chore is gone (see above).
        onDelete = vm::delete,
    )
}

@Composable
private fun ChoreDetailContent(
    ui: ChoreDetailUiState,
    onBack: () -> Unit = {},
    onToggleDone: () -> Unit = {},
    onViewRotation: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val chore = ui.chore
    val names = assigneeNames(ui.assignees, ui.meId)

    SubPage {
        BackLink(stringResource(R.string.chores_back_to_chores), onBack)
        Text(chore.title, style = RoomieType.greeting, modifier = Modifier.semantics { heading() })
        val shape = RoundedCornerShape(10.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(
                    3.dp,
                    shape,
                    ambientColor = ShadowWarm.copy(alpha = 0.08f),
                    spotColor = ShadowWarm.copy(alpha = 0.08f),
                )
                .clip(shape)
                .background(Surface)
                .border(0.7.dp, Border, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.chore_detail_assigned_to, names),
                style = RoomieType.sectionTitle.copy(lineHeight = 20.sp),
            )
            Text(
                chore.completedOn?.let { stringResource(R.string.chore_detail_completed, it.longLabel()) }
                    ?: stringResource(R.string.chore_detail_due, chore.dueDate.longLabel()),
                style = RoomieType.input.copy(color = TextSecondary, lineHeight = 20.sp),
            )
            if (ui.isRotation) {
                Text(stringResource(R.string.chore_detail_weekly_rotation), style = RoomieType.smallLink)
                if (ui.showStaysWithNote) {
                    Text(stringResource(R.string.chore_detail_stays_with, names), style = RoomieType.choreMeta)
                }
            }
        }
        PrimaryButton(
            if (chore.done) {
                stringResource(
                    R.string.chore_detail_mark_not_done,
                )
            } else {
                stringResource(R.string.chore_detail_mark_complete)
            },
            onToggleDone,
            height = 44.dp,
            gradient = true,
            corner = 10.dp,
        )
        if (ui.isRotation) {
            TanButton(stringResource(R.string.chore_detail_view_rotation), onViewRotation)
        } else {
            TextAction(
                stringResource(R.string.chore_detail_delete),
                onDelete,
                Modifier.align(Alignment.CenterHorizontally),
                style = RoomieType.linkButton.copy(color = Danger),
            )
        }
    }
}

/** "you" for the signed-in roommate, first names for everyone else. */
@Composable
private fun assigneeNames(people: List<Roommate>, me: String?): String {
    val you = stringResource(R.string.you)
    return joinNames(people.map { if (it.id == me) you else it.firstName }).ifEmpty { stringResource(R.string.nobody) }
}

/** The demo house's chores this week, for previews. */
private fun previewUi(pick: (ChoreDetailUiState) -> Boolean): ChoreDetailUiState {
    val state = FakeRoomieRepository.demoState()
    val today = LocalDate.now()
    return ChoreRules.choresInWeek(state, ChoreRules.weekStart(today), today)
        .mapNotNull { choreDetailUiState(state, it.id, today) }
        .let { all -> all.firstOrNull(pick) ?: all.first() }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ChoreDetailRotationPreview() = RoomiePreview {
    ChoreDetailContent(previewUi { it.isRotation })
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ChoreDetailOneOffPreview() = RoomiePreview {
    ChoreDetailContent(previewUi { !it.isRotation })
}
