package com.example.roomie.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import com.example.roomie.ui.components.CorkBoard
import com.example.roomie.ui.components.DateButton
import com.example.roomie.ui.components.FieldLabel
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.NoteDetailDialog
import com.example.roomie.ui.components.PillButton
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieBottomSheet
import com.example.roomie.ui.components.RoomieDatePickerDialog
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.StickyNote
import com.example.roomie.ui.components.StickySize
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.components.tiltFor
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.shortLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class NotesViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun today(): LocalDate = repo.today()
    fun addNote(text: String, due: LocalDate?, pin: Boolean) = repo.addNote(text, due, pin)
    fun togglePin(id: String) = repo.togglePin(id)
    fun deleteNote(id: String) = repo.deleteNote(id)
}

@Composable
fun NoteBoardScreen(
    startComposing: Boolean,
    onComposeHandled: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: NotesViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val me = state.session?.userId
    val today = vm.today()
    var composing by rememberSaveable { mutableStateOf(false) }
    var showingOlder by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var showOrderInfo by remember { mutableStateOf(false) }
    var openNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    fun name(id: String) = state.member(id)?.firstName ?: "Someone"

    LaunchedEffect(startComposing) {
        if (startComposing) {
            composing = true
            onComposeHandled()
        }
    }

    val active = NoteRules.boardOrder(state.notes, today)
    val older = NoteRules.older(state.notes, today)
    val shown = if (showingOlder) older else active

    Column(Modifier.fillMaxSize().background(Background)) {
        RoomieHeader("Note board") { SettingsAction(onOpenSettings) }
        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showingOlder) "Older notes, newest first" else "Pinned first, then due soon, then newest",
                    style = RoomieType.bodySecondary,
                    modifier = Modifier.weight(1f),
                )
                IconAction(R.drawable.ic_info, "How notes are ordered", { showOrderInfo = true }, iconSize = 20.dp, modifier = Modifier.padding(end = 0.dp))
            }
            CorkBoard(
                Modifier.weight(1f, fill = false).heightIn(min = 200.dp),
                onEdit = if (showingOlder) null else ({ editing = !editing }),
                editing = editing,
            ) {
                if (shown.isEmpty()) {
                    Text(
                        if (showingOlder) "No older notes." else "The board is empty. Add the first note for the house.",
                        style = RoomieType.bodySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(40.dp),
                    )
                } else {
                    NoteGrid(
                        notes = shown,
                        today = today,
                        name = ::name,
                        removableBy = if (editing) me else null,
                        onOpen = { openNoteId = it.id },
                        onRemove = { vm.deleteNote(it.id) },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showingOlder) {
                    TextAction("Back to the board", { showingOlder = false }, style = RoomieType.linkButton.copy(fontSize = 14.sp))
                } else if (older.isNotEmpty()) {
                    Row(
                        Modifier.height(44.dp).clickable(role = Role.Button) { showingOlder = true; editing = false },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Older notes (${older.size})", style = RoomieType.linkButton.copy(fontSize = 14.sp))
                        Icon(painterResource(R.drawable.ic_chevron_right_small), null, Modifier.size(16.dp), tint = Color.Unspecified)
                    }
                } else {
                    Box(Modifier)
                }
                PillButton("Add note", { composing = true })
            }
        }
    }

    if (composing) {
        NewNoteSheet(
            today = today,
            pinsInUse = NoteRules.pinsInUse(state.notes, today),
            onPost = { text, due, pin -> vm.addNote(text, due, pin); composing = false; showingOlder = false },
            onDismiss = { composing = false },
        )
    }

    if (showOrderInfo) {
        AlertDialog(
            onDismissRequest = { showOrderInfo = false },
            containerColor = Background,
            title = { Text("How notes are ordered", style = RoomieType.sheetTitle) },
            text = {
                Text(
                    "Pinned notes come first (up to 3, and pins last 7 days). Then notes with an upcoming due date, " +
                        "soonest first, then the newest notes. Unpinned notes without an upcoming date move to Older notes after 14 days.",
                    style = RoomieType.body,
                )
            },
            confirmButton = { TextAction("Got it", { showOrderInfo = false }) },
        )
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

/** Two roomy columns when the board is light, three when it fills up; scrolls and never hides notes. */
@Composable
private fun NoteGrid(
    notes: List<Note>,
    today: LocalDate,
    name: (String) -> String,
    removableBy: String?,
    onOpen: (Note) -> Unit,
    onRemove: (Note) -> Unit,
) {
    val size = if (notes.size <= 6) StickySize.Large else StickySize.Medium
    val columns = if (size == StickySize.Large) 2 else 3
    val rowHeight = if (size == StickySize.Large) 134.dp else 122.dp
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
        notes.chunked(columns).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                repeat(columns) { c ->
                    Box(Modifier.weight(1f).height(rowHeight), contentAlignment = Alignment.Center) {
                        val note = row.getOrNull(c) ?: return@Box
                        StickyNote(
                            note = note,
                            authorName = name(note.authorId),
                            pinned = note.isPinned(today),
                            size = size,
                            tilt = tiltFor(size, r * columns + c),
                            onClick = { onOpen(note) },
                            removable = removableBy != null && note.authorId == removableBy,
                            onRemove = { onRemove(note) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewNoteSheet(
    today: LocalDate,
    pinsInUse: Int,
    onPost: (String, LocalDate?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var pin by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val pinsFull = pinsInUse >= NoteRules.MAX_PINS

    RoomieBottomSheet(title = "New note", actionLabel = "Cancel", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel("Note")
            RoomieTextField(
                value = text,
                onValueChange = { text = it.take(NoteRules.MAX_LENGTH) },
                placeholder = "What should the house know?",
                height = null,
                minHeight = 96.dp,
                textStyle = RoomieType.input.copy(lineHeight = 22.4.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                contentDescription = "Note",
            )
            Text("${text.length} / ${NoteRules.MAX_LENGTH}", style = RoomieType.rowMeta, modifier = Modifier.align(Alignment.End))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Due date · optional", Modifier.weight(1f))
                if (due != null) TextAction("Clear", { due = null }, Modifier.height(24.dp), style = RoomieType.fieldLabel.copy(color = BrandBrown))
            }
            DateButton(due?.shortLabel() ?: "Add a date", { picking = true }, contentDescription = "Pick a due date")
            Text("Dated notes show under Coming up on Home until the day passes.", style = RoomieType.caption.copy(lineHeight = 16.88.sp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .alpha(if (pinsFull && !pin) 0.6f else 1f)
                .toggleable(value = pin, enabled = !pinsFull || pin, role = Role.Switch, onValueChange = { pin = it }),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_pin), null, Modifier.size(20.dp), tint = Color.Unspecified)
            Column(Modifier.weight(1f)) {
                Text("Pin to top for 7 days", style = RoomieType.input.copy(fontWeight = FontWeight.Medium))
                Text(
                    if (pinsFull) "All ${NoteRules.MAX_PINS} pins in use" else "$pinsInUse of ${NoteRules.MAX_PINS} pins in use",
                    style = RoomieType.caption,
                )
            }
            RoomieSwitch(pin)
        }
        PrimaryButton("Post note", { onPost(text, due, pin) }, enabled = text.isNotBlank())
    }

    if (picking) {
        RoomieDatePickerDialog(initial = due ?: today, today = today, onPicked = { due = it }, onDismiss = { picking = false })
    }
}

/** Visual-only switch; the parent row handles toggling and accessibility. */
@Composable
private fun RoomieSwitch(checked: Boolean) {
    Box(
        Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) BrandBrown else TextSecondary.copy(alpha = 0.35f))
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(26.dp).shadow(1.dp, CircleShape).background(Surface, CircleShape))
    }
}
