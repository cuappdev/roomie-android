package com.example.roomie.ui.home

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Roommate
import com.example.roomie.ui.chores.components.ChoreCard
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.notes.NoteItem
import com.example.roomie.ui.notes.author
import com.example.roomie.ui.notes.components.CorkBoard
import com.example.roomie.ui.notes.components.NoteDetailDialog
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.tiltFor
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.BrownOutlineSoft
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.GlassLight
import com.example.roomie.ui.theme.Inter
import com.example.roomie.ui.theme.NoteBlue
import com.example.roomie.ui.theme.NotePeach
import com.example.roomie.ui.theme.NotePurple
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowBrown
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.dayName
import com.example.roomie.ui.util.monthAbbrev
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenBoard: () -> Unit,
    onOpenChores: () -> Unit,
    onOpenChore: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    HomeContent(
        ui = ui ?: return,
        onOpenSettings = onOpenSettings,
        onOpenBoard = onOpenBoard,
        onOpenChores = onOpenChores,
        onOpenChore = onOpenChore,
        onOpenNote = vm::openNote,
        onCloseNote = vm::closeNote,
        onTogglePin = vm::togglePin,
        onDeleteNote = vm::deleteNote,
        onToggleChoreDone = vm::toggleChoreDone,
    )
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    onOpenSettings: () -> Unit = {},
    onOpenBoard: () -> Unit = {},
    onOpenChores: () -> Unit = {},
    onOpenChore: (String) -> Unit = {},
    onOpenNote: (String) -> Unit = {},
    onCloseNote: () -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onDeleteNote: (String) -> Unit = {},
    onToggleChoreDone: (String) -> Unit = {},
) {
    // The whole page scrolls, header included; only the nav bar stays put.
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        RoomieHeader(
            stringResource(
                when (ui.greeting) {
                    Greeting.Morning -> R.string.home_good_morning
                    Greeting.Afternoon -> R.string.home_good_afternoon
                    Greeting.Evening -> R.string.home_good_evening
                },
            ),
            background = Background,
            titleStyle = RoomieType.greeting,
            height = 76.dp,
            endPadding = 20.dp,
            bottomPadding = 20.dp,
        ) { SettingsGlassButton(onOpenSettings) }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RoommateStrip(ui.members)
            if (ui.comingUp.isNotEmpty()) ComingUpCard(ui.comingUp) { onOpenNote(it.note.id) }
            HomeBoard(ui.boardPreview, ui.moreNotes, onOpenBoard, onOpenNote)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_this_weeks_tasks),
                        style = RoomieType.sectionTitle,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    TextAction(stringResource(R.string.home_view_all), onOpenChores, style = RoomieType.smallLink)
                }
                if (ui.tasks.isEmpty()) {
                    Text(stringResource(R.string.home_no_tasks), style = RoomieType.bodySecondary)
                }
                ui.tasks.forEach { item ->
                    ChoreCard(
                        chore = item.chore,
                        assignees = item.assignees,
                        today = ui.today,
                        onToggle = { onToggleChoreDone(item.chore.id) },
                        onClick = { onOpenChore(item.chore.id) },
                    )
                }
            }
        }
    }

    ui.openNote?.let { open ->
        NoteDetailDialog(
            note = open.item.note,
            authorName = open.item.author(),
            pinned = open.item.pinned,
            canPin = open.canPin,
            isMine = open.item.isMine,
            onTogglePin = { onTogglePin(open.item.note.id) },
            onDelete = { onDeleteNote(open.item.note.id) },
            onDismiss = onCloseNote,
        )
    }
}

/** The small cork board: up to six notes, the last spot a "+N more" stack when there are more. */
@Composable
private fun HomeBoard(notes: List<NoteItem>, more: Int, onOpenBoard: () -> Unit, onOpenNote: (String) -> Unit) {
    CorkBoard(
        Modifier
            .height(226.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.cd_home_open_board),
                onClick = onOpenBoard,
            ),
        elevated = true,
    ) {
        if (notes.isEmpty()) {
            Text(
                stringResource(R.string.home_board_empty),
                style = RoomieType.bodySecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
            )
            return@CorkBoard
        }
        val cells: List<NoteItem?> = notes + if (more > 0) listOf(null) else emptyList()
        Column(Modifier.fillMaxSize()) {
            cells.chunked(3).forEachIndexed { r, row ->
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    repeat(3) { c ->
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            val item = row.getOrNull(c)
                            when {
                                item != null -> StickyNote(
                                    note = item.note,
                                    authorName = item.author(),
                                    pinned = item.pinned,
                                    size = StickySize.Small,
                                    tilt = tiltFor(StickySize.Small, r * 3 + c),
                                    onClick = { onOpenNote(item.note.id) },
                                )
                                c < row.size -> MoreNotesStack(more, onOpenBoard)
                            }
                        }
                    }
                }
            }
            if (cells.size <= 3) Box(Modifier.weight(1f))
        }
    }
}

@Composable
private fun RoommateStrip(members: List<Roommate>) {
    // Five roommates fill the row as in the design; smaller houses stay left-aligned.
    val arrangement = if (members.size >= 5) Arrangement.SpaceBetween else Arrangement.spacedBy(14.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
        members.forEach { m ->
            Column(
                Modifier.width(58.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BuddyAvatar(m.buddy, 48.dp, contentDescription = null)
                Text(
                    m.firstName,
                    style = RoomieType.rowMeta.copy(color = TextPrimary, lineHeight = 14.4.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ComingUpCard(notes: List<NoteItem>, onOpen: (NoteItem) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape),
    ) {
        notes.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = Divider, thickness = 1.dp)
            val due = item.note.dueDate ?: return@forEachIndexed
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.cd_home_open_note)) {
                        onOpen(item)
                    }
                    .padding(start = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d".format(due.dayOfMonth),
                        style = RoomieType.input.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 21.sp,
                        ),
                    )
                    Text(due.monthAbbrev(), style = RoomieType.rowMeta.copy(color = TextPrimary, lineHeight = 12.6.sp))
                }
                Box(Modifier.width(1.dp).height(34.dp).background(TextPrimary))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            if (item.pinned) R.string.home_coming_up_pinned else R.string.home_coming_up_note,
                            item.author(),
                        ),
                        style = RoomieType.rowMeta.copy(lineHeight = 15.6.sp),
                        maxLines = 1,
                    )
                    val on = stringResource(R.string.home_coming_up_on, due.dayName())
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                append(item.note.text.lineSequence().first())
                            }
                            append(on)
                        },
                        style = RoomieType.body.copy(fontSize = 13.5.sp, lineHeight = 17.55.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = Color.Unspecified)
            }
        }
    }
}

/** "+N more" stack on Home that opens the full board. */
@Composable
private fun MoreNotesStack(count: Int, onClick: () -> Unit) {
    Box(Modifier.size(width = 84.dp, height = 78.dp)) {
        Box(
            Modifier
                .stackedNote(3.dp, 2.dp)
                .rotate(5f)
                .shadow(2.dp, RectangleShape, clip = false)
                .background(NotePurple),
        )
        Box(
            Modifier
                .stackedNote(0.dp, 0.dp)
                .rotate(-3f)
                .shadow(2.dp, RectangleShape, clip = false)
                .background(NoteBlue),
        )
        Column(
            Modifier
                .size(width = 84.dp, height = 78.dp)
                .shadow(3.dp, RectangleShape, clip = false, ambientColor = ShadowBrown, spotColor = ShadowBrown)
                .background(NotePeach)
                .clickable(
                    role = Role.Button,
                    onClickLabel = pluralStringResource(R.plurals.cd_more_notes, count, count),
                    onClick = onClick,
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.more_notes_count, count),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 1.em,
                    color = BrandBrown,
                ),
            )
            Text(
                stringResource(R.string.more_notes),
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = BrandBrown,
                ),
            )
        }
    }
}

private fun Modifier.stackedNote(x: Dp, y: Dp): Modifier = offset(x, y).size(width = 84.dp, height = 78.dp)

/** 38dp glass settings button from the Home header. */
@Composable
private fun SettingsGlassButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .padding(bottom = 2.dp)
            .size(38.dp)
            .clip(shape)
            .background(GlassLight)
            .border(1.dp, BrownOutlineSoft, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_settings),
            stringResource(R.string.settings),
            Modifier.size(20.dp),
            tint = Color.Unspecified,
        )
    }
}

private fun previewUi(openFirstNote: Boolean = false): HomeUiState {
    val state = FakeRoomieRepository.demoState()
    val openId = if (openFirstNote) state.notes.firstOrNull()?.id else null
    return homeUiState(state, openId, LocalDate.now(), LocalDateTime.now())!!
}

@Preview(widthDp = 390, heightDp = 1100)
@Composable
private fun HomeContentPreview() = RoomiePreview {
    HomeContent(previewUi())
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HomeContentNoteOpenPreview() = RoomiePreview {
    HomeContent(previewUi(openFirstNote = true))
}

@Preview
@Composable
private fun MoreNotesStackPreview() = RoomiePreview(padding = 16.dp) {
    MoreNotesStack(4) {}
}
