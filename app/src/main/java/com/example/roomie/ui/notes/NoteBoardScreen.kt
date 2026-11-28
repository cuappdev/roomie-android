package com.example.roomie.ui.notes

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.NoteRules
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.SettingsAction
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.notes.components.CorkBoard
import com.example.roomie.ui.notes.components.DeleteNoteDialog
import com.example.roomie.ui.notes.components.HoldAction
import com.example.roomie.ui.notes.components.HoldOverlay
import com.example.roomie.ui.notes.components.InlineNoteEditor
import com.example.roomie.ui.notes.components.LeaveNoteScreen
import com.example.roomie.ui.notes.components.NoteDetailDialog
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.boundsIn
import com.example.roomie.ui.notes.components.holdActionsFor
import com.example.roomie.ui.notes.components.holdTargets
import com.example.roomie.ui.notes.components.holdTargetsPx
import com.example.roomie.ui.notes.components.nearestTarget
import com.example.roomie.ui.notes.components.tapOrHold
import com.example.roomie.ui.notes.components.tiltFor
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import java.time.LocalDate

@Composable
fun NoteBoardScreen(
    startComposing: Boolean,
    onComposeHandled: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: NotesViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pinsFull = stringResource(R.string.notes_pins_full, NoteRules.MAX_PINS)

    LaunchedEffect(startComposing) {
        if (startComposing) {
            vm.startComposing()
            onComposeHandled()
        }
    }
    LaunchedEffect(Unit) {
        vm.pinsFullEvents.collect { Toast.makeText(context, pinsFull, Toast.LENGTH_SHORT).show() }
    }

    val composing = ui.composing
    if (composing != null) {
        LeaveNoteScreen(composing, vm::onLeaveNoteEvent)
        return
    }
    NoteBoardContent(
        ui = ui,
        onOpenSettings = onOpenSettings,
        onAddNote = vm::startComposing,
        onShowOlder = vm::showOlder,
        onOpenNote = vm::openNote,
        onCloseNote = vm::closeNote,
        onTogglePin = vm::togglePin,
        onDeleteOpenNote = vm::deleteOpenNote,
        onHoldAction = vm::onHoldAction,
        onSaveEdit = vm::saveEdit,
        onCancelEdit = vm::cancelEdit,
        onConfirmDelete = vm::confirmDelete,
        onCancelDelete = vm::cancelDelete,
    )
}

@Composable
private fun NoteBoardContent(
    ui: NoteBoardUiState,
    onOpenSettings: () -> Unit = {},
    onAddNote: () -> Unit = {},
    onShowOlder: (Boolean) -> Unit = {},
    onOpenNote: (String) -> Unit = {},
    onCloseNote: () -> Unit = {},
    onTogglePin: (String) -> Unit = {},
    onDeleteOpenNote: () -> Unit = {},
    onHoldAction: (String, HoldAction) -> Unit = { _, _ -> },
    onSaveEdit: (String, LocalDate?) -> Unit = { _, _ -> },
    onCancelEdit: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onCancelDelete: () -> Unit = {},
) {
    val density = LocalDensity.current
    var showOrderInfo by remember { mutableStateOf(false) }
    // Press-and-hold menu: the held note, where it sits, and where the finger is.
    var holdId by remember { mutableStateOf<String?>(null) }
    var holdBounds by remember { mutableStateOf(Rect.Zero) }
    var holdPointer by remember { mutableStateOf<Offset?>(null) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    fun pick(id: String, action: HoldAction) {
        holdId = null
        holdPointer = null
        onHoldAction(id, action)
    }

    Box(Modifier.fillMaxSize().onGloballyPositioned { rootCoords = it }) {
        // The whole page scrolls, header included; only the nav bar stays put.
        Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
            RoomieHeader(
                stringResource(if (ui.showingOlder) R.string.notes_older_title else R.string.notes_title),
                background = Background,
                titleStyle = RoomieType.greeting,
                height = 76.dp,
            ) { SettingsAction(onOpenSettings) }
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!ui.showingOlder) {
                    PrimaryButton(
                        stringResource(R.string.notes_add),
                        onAddNote,
                        height = 40.dp,
                        gradient = true,
                        corner = 10.dp,
                        icon = R.drawable.ic_plus_white,
                        textStyle = RoomieType.smallLink.copy(color = Surface),
                    )
                }
                CorkBoard(Modifier.heightIn(min = 484.dp)) {
                    if (ui.notes.isEmpty()) {
                        Text(
                            stringResource(if (ui.showingOlder) R.string.notes_no_older else R.string.notes_empty),
                            style = RoomieType.bodySecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(40.dp),
                        )
                    } else {
                        NoteGrid(
                            notes = ui.notes,
                            hiddenId = holdId,
                            onOpen = { onOpenNote(it.note.id) },
                            onHoldStart = { item, coords ->
                                val root = rootCoords ?: return@NoteGrid
                                holdBounds = coords.boundsIn(root)
                                holdPointer = null
                                holdId = item.note.id
                            },
                            onHoldMove = { coords, position ->
                                val root = rootCoords ?: return@NoteGrid
                                holdPointer = root.localPositionOf(coords, position)
                            },
                            onHoldEnd = { item ->
                                val root = rootCoords
                                val actions = holdActionsFor(item.isMine)
                                val targets = if (root != null) {
                                    holdTargetsPx(holdBounds, root.size.width.toFloat(), actions, density.density)
                                } else {
                                    emptyMap()
                                }
                                val target = holdPointer?.let {
                                    nearestTarget(targets, it, with(density) { HOLD_RADIUS.toPx() })
                                }
                                holdPointer = null
                                if (target != null) pick(item.note.id, target)
                            },
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (ui.showingOlder) {
                        TextAction(
                            stringResource(R.string.notes_back_to_board),
                            { onShowOlder(false) },
                            style = RoomieType.linkButton.copy(fontSize = 14.sp),
                        )
                    } else if (ui.olderCount > 0) {
                        Row(
                            Modifier.height(44.dp).clickable(role = Role.Button) { onShowOlder(true) },
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.notes_older_count, ui.olderCount),
                                style = RoomieType.linkButton.copy(fontSize = 14.sp),
                            )
                            Icon(
                                painterResource(R.drawable.ic_chevron_right_small),
                                null,
                                Modifier.size(16.dp),
                                tint = Color.Unspecified,
                            )
                        }
                    }
                    Box(Modifier.weight(1f))
                    IconAction(R.drawable.ic_info, stringResource(R.string.notes_how_it_works), {
                        showOrderInfo = true
                    }, iconSize = 20.dp)
                }
            }
        }

        ui.notes.firstOrNull { it.note.id == holdId }?.let { held ->
            val targets = holdTargets(holdBounds, rootCoords?.size?.width?.toFloat() ?: 0f, holdActionsFor(held.isMine))
            val radius = with(density) { HOLD_RADIUS.toPx() }
            HoldOverlay(
                note = held.note,
                authorName = held.author(),
                pinned = held.pinned,
                size = boardNoteSize(ui.notes.size),
                bounds = holdBounds,
                targets = targets,
                highlighted = holdPointer?.let { nearestTarget(targets, it, radius) },
                onPick = { pick(held.note.id, it) },
                onDismiss = {
                    holdId = null
                    holdPointer = null
                },
            )
        }

        ui.editing?.let { editing ->
            InlineNoteEditor(
                note = editing.note,
                authorName = editing.author(),
                today = ui.today,
                onCancel = onCancelEdit,
                onSave = onSaveEdit,
            )
        }
    }

    ui.deleting?.let { DeleteNoteDialog(it.note, onCancel = onCancelDelete, onDelete = onConfirmDelete) }

    if (showOrderInfo) {
        AlertDialog(
            onDismissRequest = { showOrderInfo = false },
            containerColor = Background,
            title = { Text(stringResource(R.string.notes_how_it_works), style = RoomieType.sheetTitle) },
            text = {
                Text(
                    stringResource(
                        R.string.notes_how_it_works_body,
                        NoteRules.MAX_PINS,
                        NoteRules.PIN_DAYS,
                        NoteRules.OLDER_AFTER_DAYS,
                    ),
                    style = RoomieType.body,
                )
            },
            confirmButton = { TextAction(stringResource(R.string.notes_got_it), { showOrderInfo = false }) },
        )
    }

    ui.openNote?.let { open ->
        NoteDetailDialog(
            note = open.item.note,
            authorName = open.item.author(),
            pinned = open.item.pinned,
            canPin = open.canPin,
            isMine = open.item.isMine,
            onTogglePin = { onTogglePin(open.item.note.id) },
            onDelete = onDeleteOpenNote,
            onDismiss = onCloseNote,
        )
    }
}

/** How close the finger has to be to a hold action to pick it. */
private val HOLD_RADIUS = 44.dp

private fun boardNoteSize(count: Int) = if (count <= 6) StickySize.Large else StickySize.Medium

/**
 * Two roomy columns when the board is light, three when it fills up. The board grows with its
 * notes and the page scrolls, so nothing is hidden. Each note opens on tap and lifts into the
 * action menu on press-and-hold.
 */
@Composable
private fun NoteGrid(
    notes: List<NoteItem>,
    hiddenId: String?,
    onOpen: (NoteItem) -> Unit,
    onHoldStart: (NoteItem, LayoutCoordinates) -> Unit,
    onHoldMove: (LayoutCoordinates, Offset) -> Unit,
    onHoldEnd: (NoteItem) -> Unit,
) {
    val size = boardNoteSize(notes.size)
    val columns = if (size == StickySize.Large) 2 else 3
    val rowHeight = if (size == StickySize.Large) 150.dp else 122.dp
    val latestStart by rememberUpdatedState(onHoldStart)
    val latestMove by rememberUpdatedState(onHoldMove)
    val latestEnd by rememberUpdatedState(onHoldEnd)
    val latestOpen by rememberUpdatedState(onOpen)
    val readNote = stringResource(R.string.cd_note_read)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        notes.chunked(columns).forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().height(rowHeight)) {
                repeat(columns) { c ->
                    Box(Modifier.weight(1f).height(rowHeight), contentAlignment = Alignment.Center) {
                        val item = row.getOrNull(c) ?: return@Box
                        var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                        StickyNote(
                            note = item.note,
                            authorName = item.author(),
                            pinned = item.pinned,
                            size = size,
                            tilt = tiltFor(size, r * columns + c),
                            onClick = null,
                            modifier = Modifier
                                .onGloballyPositioned { coords = it }
                                .alpha(if (item.note.id == hiddenId) 0f else 1f)
                                .semantics {
                                    onClick(label = readNote) {
                                        latestOpen(item)
                                        true
                                    }
                                }
                                .tapOrHold(
                                    key = item.note.id,
                                    onTap = { latestOpen(item) },
                                    onHoldStart = { coords?.let { latestStart(item, it) } },
                                    onHoldMove = { p -> coords?.let { latestMove(it, p) } },
                                    onHoldEnd = { latestEnd(item) },
                                ),
                        )
                    }
                }
            }
        }
    }
}

private fun previewUi(selection: BoardSelection = BoardSelection()): NoteBoardUiState =
    noteBoardUiState(FakeRoomieRepository.demoState(), selection, LocalDate.now())

@Preview(widthDp = 390, heightDp = 1000)
@Composable
private fun NoteBoardPreview() = RoomiePreview {
    NoteBoardContent(previewUi())
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun NoteBoardOlderPreview() = RoomiePreview {
    NoteBoardContent(previewUi(BoardSelection(showingOlder = true)))
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun NoteBoardEmptyPreview() = RoomiePreview {
    NoteBoardContent(
        noteBoardUiState(FakeRoomieRepository.demoState().copy(notes = emptyList()), BoardSelection(), LocalDate.now()),
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun NoteBoardDeletePreview() = RoomiePreview {
    val first = FakeRoomieRepository.demoState().notes.first().id
    NoteBoardContent(previewUi(BoardSelection(deletingId = first)))
}
