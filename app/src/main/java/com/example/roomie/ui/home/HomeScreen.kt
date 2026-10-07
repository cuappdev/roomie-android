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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Note
import com.example.roomie.data.NoteRules
import com.example.roomie.data.RoomieState
import com.example.roomie.data.Roommate
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.CorkBoard
import com.example.roomie.ui.components.DayTaskRow
import com.example.roomie.ui.components.MoreNotesStack
import com.example.roomie.ui.components.NoteDetailDialog
import com.example.roomie.ui.components.PillButton
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.StickyNote
import com.example.roomie.ui.components.StickySize
import com.example.roomie.ui.components.tiltFor
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.dayName
import com.example.roomie.ui.util.monthAbbrev
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun today(): LocalDate = repo.today()
    fun togglePin(id: String) = repo.togglePin(id)
    fun deleteNote(id: String) = repo.deleteNote(id)
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenBoard: () -> Unit,
    onAddNote: () -> Unit,
    onOpenChores: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val house = state.house ?: return
    val me = state.session?.userId
    val today = vm.today()
    var openNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    fun name(id: String) = state.member(id)?.firstName ?: "Someone"

    Column(Modifier.fillMaxSize().background(Background)) {
        RoomieHeader("Roomie") { SettingsAction(onOpenSettings) }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RoommateStrip(house.members)

            val comingUp = NoteRules.comingUp(state.notes, today)
            if (comingUp.isNotEmpty()) {
                ComingUpCard(comingUp, today, ::name, { openNoteId = it.id })
            }

            val (preview, more) = NoteRules.homePreview(state.notes, today)
            CorkBoard(Modifier.height(226.dp), onEdit = onOpenBoard) {
                if (preview.isEmpty()) {
                    Text(
                        "No notes yet. Add one for the house.",
                        style = RoomieType.bodySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
                    )
                } else {
                    val cells: List<Note?> = preview + if (more > 0) listOf(null) else emptyList()
                    Column(Modifier.fillMaxSize()) {
                        cells.chunked(3).forEachIndexed { r, row ->
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                repeat(3) { c ->
                                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        val note = row.getOrNull(c)
                                        val index = r * 3 + c
                                        when {
                                            note != null -> StickyNote(
                                                note = note,
                                                authorName = name(note.authorId),
                                                pinned = note.isPinned(today),
                                                size = StickySize.Small,
                                                tilt = tiltFor(StickySize.Small, index),
                                                onClick = { openNoteId = note.id },
                                                showDueDate = false,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                PillButton("Add note", onAddNote)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This Week's Tasks", style = RoomieType.sectionTitle, modifier = Modifier.semantics { heading() })
                val endOfWeek = FakeRoomieRepository.weekOf(today).last()
                val tasks = state.chores
                    .filter { !it.done && !it.dueDate.isBefore(today) && !it.dueDate.isAfter(endOfWeek) }
                    .sortedBy { it.dueDate }
                if (tasks.isEmpty()) {
                    Text("Nothing else due this week.", style = RoomieType.bodySecondary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { chore ->
                        DayTaskRow(
                            day = if (chore.dueDate == today) "Tonight" else chore.dueDate.dayName(),
                            task = chore.title,
                            who = if (chore.assigneeId == me) "You" else name(chore.assigneeId),
                            onClick = onOpenChores,
                        )
                    }
                }
            }
        }
    }

    val open = state.notes.firstOrNull { it.id == openNoteId }
    if (open != null) {
        NoteDetailDialog(
            note = open,
            authorName = name(open.authorId),
            pinned = open.isPinned(today),
            canPin = NoteRules.pinsInUse(state.notes, today) < NoteRules.MAX_PINS,
            isMine = open.authorId == me,
            onTogglePin = { vm.togglePin(open.id) },
            onDelete = { vm.deleteNote(open.id); openNoteId = null },
            onDismiss = { openNoteId = null },
        )
    }
}

@Composable
private fun RoommateStrip(members: List<Roommate>) {
    // Five roommates fill the row as in the design; smaller houses stay left-aligned.
    val arrangement = if (members.size >= 5) Arrangement.SpaceBetween else Arrangement.spacedBy(14.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = arrangement) {
        members.forEach { m ->
            Column(Modifier.width(58.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
private fun ComingUpCard(notes: List<Note>, today: LocalDate, name: (String) -> String, onOpen: (Note) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape),
    ) {
        notes.forEachIndexed { i, note ->
            if (i > 0) HorizontalDivider(color = Divider, thickness = 1.dp)
            val due = note.dueDate!!
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(role = Role.Button, onClickLabel = "Open note") { onOpen(note) }
                    .padding(start = 12.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%02d".format(due.dayOfMonth), style = RoomieType.input.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 21.sp))
                    Text(due.monthAbbrev(), style = RoomieType.rowMeta.copy(color = TextPrimary, lineHeight = 12.6.sp))
                }
                Box(Modifier.width(1.dp).height(34.dp).background(TextPrimary))
                Column(Modifier.weight(1f)) {
                    val source = if (note.isPinned(today)) "pinned note" else "note"
                    Text("Coming up · from ${name(note.authorId)}'s $source", style = RoomieType.rowMeta.copy(lineHeight = 15.6.sp), maxLines = 1)
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(note.text.lineSequence().first()) }
                            append(" on ${due.dayName()}")
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
