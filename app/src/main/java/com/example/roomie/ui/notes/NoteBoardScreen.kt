package com.example.roomie.ui.notes

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import com.example.roomie.ui.components.DropdownField
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.NoteDetailDialog
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.StickyNote
import com.example.roomie.ui.components.StickySize
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.components.tiltFor
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.fullLabel
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

    // "Leave a Note" is a full page in the design; it replaces the board and keeps the nav bar.
    if (composing) {
        LeaveNoteScreen(
            today = today,
            pinsInUse = NoteRules.pinsInUse(state.notes, today),
            onPost = { text, due, pin -> vm.addNote(text, due, pin); composing = false; showingOlder = false },
            onCancel = { composing = false },
        )
        return
    }

    val active = NoteRules.boardOrder(state.notes, today)
    val older = NoteRules.older(state.notes, today)
    val shown = if (showingOlder) older else active

    // The whole page scrolls, header included; only the nav bar stays put.
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        RoomieHeader(
            if (showingOlder) "Older Notes" else "Note Board",
            background = Background,
            titleStyle = RoomieType.greeting,
            height = 76.dp,
        ) { SettingsAction(onOpenSettings) }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!showingOlder) {
                PrimaryButton(
                    "Add Note",
                    { composing = true },
                    height = 40.dp,
                    gradient = true,
                    corner = 10.dp,
                    icon = R.drawable.ic_plus_white,
                    textStyle = RoomieType.smallLink.copy(color = Surface),
                )
            }
            CorkBoard(
                Modifier.heightIn(min = 484.dp),
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                }
                Box(Modifier.weight(1f))
                IconAction(R.drawable.ic_info, "How notes are ordered", { showOrderInfo = true }, iconSize = 20.dp)
            }
        }
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

/**
 * Two roomy columns when the board is light, three when it fills up. The board grows with its
 * notes and the page scrolls, so nothing is hidden.
 */
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
    val rowHeight = if (size == StickySize.Large) 150.dp else 122.dp
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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
private fun LeaveNoteScreen(
    today: LocalDate,
    pinsInUse: Int,
    onPost: (String, LocalDate?, Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var pin by rememberSaveable { mutableStateOf(false) }
    // Not wired to anything yet: the app has no notifications.
    var notify by rememberSaveable { mutableStateOf(false) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    val pinsFull = pinsInUse >= NoteRules.MAX_PINS
    val sectionLabel = RoomieType.cardTitle.copy(color = TextSecondary)

    BackHandler(onBack = onCancel)

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        RoomieHeader("Leave a Note", background = Background, titleStyle = RoomieType.greeting, height = 76.dp)
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Message", style = sectionLabel)
                RoomieTextField(
                    value = text,
                    onValueChange = { text = it.take(NoteRules.MAX_LENGTH) },
                    placeholder = "Type Here...",
                    height = null,
                    minHeight = 72.dp,
                    textStyle = RoomieType.input.copy(lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    contentDescription = "Message",
                )
                // The design has no counter; show one only once the 140-character limit is close.
                if (text.length > NoteRules.MAX_LENGTH - 20) {
                    Text("${text.length} / ${NoteRules.MAX_LENGTH}", style = RoomieType.rowMeta, modifier = Modifier.align(Alignment.End))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            append("Due Date · ")
                            withStyle(SpanStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal)) { append("Optional") }
                        },
                        style = sectionLabel,
                        modifier = Modifier.weight(1f),
                    )
                    if (due != null) {
                        TextAction("Clear", { due = null; calendarOpen = false }, Modifier.height(24.dp), style = RoomieType.smallLink)
                    }
                }
                DropdownField(
                    text = due?.fullLabel(),
                    placeholder = "Select a due date",
                    expanded = calendarOpen,
                    onClick = { calendarOpen = !calendarOpen },
                    contentDescription = "Pick a due date",
                )
                if (calendarOpen) {
                    RoomieInlineCalendar(selected = due, today = today, onPick = { due = it; calendarOpen = false })
                }
                Text("Notes stay on the board for ${NoteRules.OLDER_AFTER_DAYS} days.", style = RoomieType.onboardingNote)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SwitchRow("Notify Others", notify, onChange = { notify = it })
                SwitchRow(
                    "Pin Note",
                    pin,
                    onChange = { pin = it },
                    enabled = !pinsFull || pin,
                    helper = if (pinsFull && !pin) "All ${NoteRules.MAX_PINS} pins in use" else null,
                )
            }
            PrimaryButton("Post Note", { onPost(text, due, pin) }, enabled = text.isNotBlank(), height = 44.dp, gradient = true, corner = 10.dp)
            GlassButton("Cancel", onCancel)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true, helper: String? = null) {
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
