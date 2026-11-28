package com.example.roomie.ui.chores

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.example.roomie.ui.chores.components.AddChoreSheet
import com.example.roomie.ui.chores.components.ChoreCard
import com.example.roomie.ui.chores.components.GlassFaint
import com.example.roomie.ui.chores.components.SegmentedControl
import com.example.roomie.ui.chores.components.WeekPicker
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.WhiteBorderWidth
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.CalendarDayEnd
import com.example.roomie.ui.theme.CalendarDayStart
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.util.weekRangeLabel
import java.time.LocalDate

@Composable
fun ChoresScreen(
    onOpenSettings: () -> Unit,
    onOpenChore: (String) -> Unit,
    onManageRotations: () -> Unit,
    vm: ChoresViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ChoresContent(
        ui = ui ?: return,
        onOpenSettings = onOpenSettings,
        onOpenChore = onOpenChore,
        onManageRotations = onManageRotations,
        onPickDate = vm::showWeekOf,
        onPreviousWeek = vm::previousWeek,
        onNextWeek = vm::nextWeek,
        onMineOnlyChange = vm::setMineOnly,
        onToggleDone = vm::toggleDone,
        onAddChore = vm::openAddChore,
        onAddChoreEvent = vm::onAddChoreEvent,
    )
}

@Composable
private fun ChoresContent(
    ui: ChoresUiState,
    onOpenSettings: () -> Unit = {},
    onOpenChore: (String) -> Unit = {},
    onManageRotations: () -> Unit = {},
    onPickDate: (LocalDate) -> Unit = {},
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onMineOnlyChange: (Boolean) -> Unit = {},
    onToggleDone: (String) -> Unit = {},
    onAddChore: () -> Unit = {},
    onAddChoreEvent: (AddChoreEvent) -> Unit = {},
) {
    var calendarExpanded by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RoomieHeader(
                stringResource(R.string.chores_title),
                background = Background,
                titleStyle = RoomieType.greeting,
                height = 76.dp,
            ) {
                SettingsAction(onOpenSettings)
            }
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddChoreBar(onAddChore)
                WeekPicker(
                    week = ui.week,
                    today = ui.today,
                    expanded = calendarExpanded,
                    onToggleExpanded = { calendarExpanded = !calendarExpanded },
                    onPickDate = onPickDate,
                    onPrevious = onPreviousWeek,
                    onNext = onNextWeek,
                )
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (ui.isThisWeek) {
                            stringResource(
                                R.string.chores_this_week,
                            )
                        } else {
                            ui.week.weekRangeLabel(short = true)
                        },
                        style = RoomieType.cardTitle.copy(lineHeight = 26.sp),
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    SmallGlassButton(stringResource(R.string.chores_manage_rotations), onManageRotations)
                }
                SegmentedControl(
                    listOf(
                        stringResource(R.string.chores_mine),
                        stringResource(R.string.chores_all),
                    ),
                    if (ui.mineOnly) 0 else 1,
                    {
                        onMineOnlyChange(
                            it == 0,
                        )
                    },
                )
                if (ui.chores.isEmpty()) {
                    Text(
                        if (ui.mineOnly) {
                            stringResource(
                                R.string.chores_none_mine,
                            )
                        } else {
                            stringResource(R.string.chores_none)
                        },
                        style = RoomieType.choreMeta,
                    )
                }
                ui.chores.forEach { item ->
                    ChoreCard(
                        chore = item.chore,
                        assignees = item.assignees,
                        today = ui.today,
                        onToggle = { onToggleDone(item.chore.id) },
                        onClick = { onOpenChore(item.chore.id) },
                    )
                }
            }
        }

        ui.addChore?.let { AddChoreSheet(it, onAddChoreEvent) }
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
            .shadow(
                3.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.12f),
                spotColor = ShadowWarm.copy(alpha = 0.12f),
            )
            .clip(shape)
            .background(Background)
            .background(Brush.horizontalGradient(listOf(CalendarDayStart, CalendarDayEnd)))
            .border(WhiteBorderWidth, Color.White.copy(alpha = 0.82f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.chores_add), style = RoomieType.glassButton, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_plus_brown), null, Modifier.size(16.dp), tint = Color.Unspecified)
    }
}

/** Small glass button with brown text ("Manage Rotations"). */
@Composable
private fun SmallGlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .height(34.dp)
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.07f),
                spotColor = ShadowWarm.copy(alpha = 0.07f),
            )
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(1.dp, Tan.copy(alpha = 0.55f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = RoomieType.smallLink.copy(lineHeight = 18.sp))
    }
}

private fun previewUi(mineOnly: Boolean = false, adding: Boolean = false): ChoresUiState {
    val state = FakeRoomieRepository.demoState()
    val today = LocalDate.now()
    val form = if (adding) newAddChoreForm(state, today) else null
    return choresUiState(state, ChoreRules.weekStart(today), mineOnly, form, today)!!
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ChoresContentPreview() = RoomiePreview {
    ChoresContent(previewUi())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ChoresContentMineOnlyPreview() = RoomiePreview {
    ChoresContent(previewUi(mineOnly = true))
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ChoresContentAddingPreview() = RoomiePreview {
    ChoresContent(previewUi(adding = true))
}

@Preview
@Composable
private fun AddChoreBarPreview() = RoomiePreview(padding = 16.dp) {
    AddChoreBar {}
}
