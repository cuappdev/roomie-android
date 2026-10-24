package com.example.roomie.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.data.NoteRules
import com.example.roomie.ui.components.BrandGradient
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.components.StickyNote
import com.example.roomie.ui.components.StickySize
import com.example.roomie.ui.components.TackView
import com.example.roomie.ui.components.color
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.GlassFaintEnd
import com.example.roomie.ui.theme.GlassFaintStart
import com.example.roomie.ui.theme.Poppins
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.shortLabel
import java.time.LocalDate
import kotlin.math.hypot

enum class HoldAction(val icon: Int, val title: String, val hint: String) {
    Edit(R.drawable.ic_hold_edit, "Edit Note", "Release to edit"),
    Delete(R.drawable.ic_hold_delete, "Delete Note", "Release to delete"),
    Pin(R.drawable.ic_hold_pin, "Pin Note", "Release to pin"),
}

private val HoldScrim = Color(0xD60A0908)
private val EditButtonFill = Color(0xFFFFFAF2)

/**
 * Tap opens the note; press and hold lifts it into the action menu. The finger can keep moving after
 * the hold so the user can slide straight onto an action and release, as in the Figma prototype.
 * [onHoldMove] gets positions in [coordinates]' local space.
 */
fun Modifier.tapOrHold(
    key: Any,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldMove: (Offset) -> Unit,
    onHoldEnd: () -> Unit,
): Modifier = pointerInput(key) {
    awaitEachGesture {
        val down = awaitFirstDown()
        var up: PointerInputChange? = null
        val settled = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            up = waitForUpOrCancellation()
            true
        }
        if (settled == true) {
            // Released before the hold kicked in (a tap), or the board scrolled away.
            if (up != null) onTap()
            return@awaitEachGesture
        }
        onHoldStart()
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            change.consume()
            if (!change.pressed) break
            onHoldMove(change.position)
        }
        onHoldEnd()
    }
}

/** Where a held note sits, in the board overlay's coordinates. */
fun LayoutCoordinates.boundsIn(root: LayoutCoordinates): Rect {
    val topLeft = root.localPositionOf(this, Offset.Zero)
    return Rect(topLeft, Size(size.width.toFloat(), size.height.toFloat()))
}

// Offsets in dp from the Figma frame, measured from the note's top corner on the side the menu opens.
private val HOLD_SLOTS = mapOf(HoldAction.Edit to Offset(-48f, -7f), HoldAction.Delete to Offset(-62f, 65f), HoldAction.Pin to Offset(-28f, 141f))

/** Action button centres around a held note, fanned out on whichever side has room. */
fun holdTargetsPx(bounds: Rect, overlayWidth: Float, actions: List<HoldAction>, density: Float): Map<HoldAction, Offset> {
    val openLeft = bounds.center.x > overlayWidth / 2
    return actions.associateWith { action ->
        val slot = if (actions.size == 1) Offset(-56f, 60f) else HOLD_SLOTS.getValue(action)
        val x = if (openLeft) bounds.left + slot.x * density else bounds.right - slot.x * density
        Offset(x, bounds.top + slot.y * density)
    }
}

@Composable
fun holdTargets(bounds: Rect, overlayWidth: Float, actions: List<HoldAction>): Map<HoldAction, Offset> =
    holdTargetsPx(bounds, overlayWidth, actions, LocalDensity.current.density)

fun nearestTarget(targets: Map<HoldAction, Offset>, point: Offset, radiusPx: Float): HoldAction? =
    targets.minByOrNull { (_, c) -> hypot(c.x - point.x, c.y - point.y) }
        ?.takeIf { (_, c) -> hypot(c.x - point.x, c.y - point.y) <= radiusPx }?.key

/**
 * "Hold / Choose an action": the board dims, the note lifts, and Edit, Delete and Pin fan out beside
 * it. While the finger is still down, sliding onto an action highlights it and releasing picks it.
 * After a plain release the menu stays open and each action can be tapped; tapping the dark
 * background cancels.
 */
@Composable
fun HoldOverlay(
    note: Note,
    authorName: String,
    pinned: Boolean,
    size: StickySize,
    bounds: Rect,
    targets: Map<HoldAction, Offset>,
    highlighted: HoldAction?,
    onPick: (HoldAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(HoldScrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClickLabel = "Cancel", onClick = onDismiss),
        )
        StickyNote(
            note = note,
            authorName = authorName,
            pinned = pinned,
            size = size,
            tilt = -1f,
            onClick = null,
            modifier = Modifier
                .offset { IntOffset(bounds.left.toInt(), bounds.top.toInt()) }
                .graphicsLayer { scaleX = 1.06f; scaleY = 1.06f },
        )
        targets.forEach { (action, center) ->
            val active = action == highlighted
            val size = if (active) 64.dp else 56.dp
            val half = with(density) { (size / 2).toPx() }
            Box(
                Modifier
                    .offset { IntOffset((center.x - half).toInt(), (center.y - half).toInt()) }
                    .size(size)
                    .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.24f), spotColor = Color.Black.copy(alpha = 0.24f))
                    .clip(CircleShape)
                    .background(if (active) Surface else BrandBrown)
                    .border(1.dp, Color.White.copy(alpha = if (active) 1f else 0.24f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = action.label(pinned)) { onPick(action) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(action.icon), action.label(pinned), Modifier.size(if (active) 27.dp else 24.dp), tint = if (active) BrandBrown else Surface)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 72.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(highlighted?.label(pinned) ?: "Choose an Action", style = RoomieType.greeting.copy(color = Surface))
            Text(
                highlighted?.let { if (it == HoldAction.Pin && pinned) "Release to unpin" else it.hint }
                    ?: if (targets.size == 1) "Slide to Pin" else "Slide to Edit, Delete, or Pin",
                style = RoomieType.input.copy(color = Surface.copy(alpha = 0.8f)),
            )
        }
    }
}

private fun HoldAction.label(pinned: Boolean) = if (this == HoldAction.Pin && pinned) "Unpin Note" else title

/**
 * "Inline Edit": the note opens large over the dimmed board with the keyboard up. Cancel and Save
 * sit above it; the button below adds, changes or removes the due date.
 */
@Composable
fun InlineNoteEditor(
    note: Note,
    authorName: String,
    today: LocalDate,
    onCancel: () -> Unit,
    onSave: (String, LocalDate?) -> Unit,
) {
    // Cursor starts at the end so the user can keep typing, as in the Figma keyboard frame.
    var text by rememberSaveable(note.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(note.text, TextRange(note.text.length)))
    }
    var due by rememberSaveable(note.id) { mutableStateOf(note.dueDate) }
    var calendarOpen by rememberSaveable(note.id) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(note.id) {
        focus.requestFocus()
        keyboard?.show()
    }
    BackHandler(onBack = onCancel)

    Box(
        Modifier
            .fillMaxSize()
            .background(HoldScrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                SmallEditButton("Cancel", onCancel, fill = SolidColor(EditButtonFill), textColor = BrandBrown, border = Tan)
                Spacer(Modifier.weight(1f))
                SmallEditButton("Save", { onSave(text.text, due) }, fill = BrandGradient, textColor = Surface, border = BrandBrown.copy(alpha = 0.85f), enabled = text.text.isNotBlank())
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 21.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .shadow(10.dp, RectangleShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                        .background(note.color.color())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.text.length <= NoteRules.MAX_LENGTH) text = it },
                        modifier = Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                        textStyle = TextStyle(fontFamily = Poppins, fontSize = 20.sp, lineHeight = 28.sp, color = TextPrimary),
                        cursorBrush = SolidColor(BrandBrown),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                    Text(
                        "- $authorName",
                        style = TextStyle(fontFamily = Poppins, fontSize = 14.sp, color = TextPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
                TackView(note.tack, Modifier.align(Alignment.TopCenter).offset(y = (-8).dp))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 21.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(shape)
                        .background(EditButtonFill)
                        .border(1.dp, Tan, shape)
                        .clickable(role = Role.Button) { calendarOpen = !calendarOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        due?.let { "${it.shortLabel()} · Change Date" } ?: "+ Add Due Date",
                        style = RoomieType.pillButton.copy(fontWeight = FontWeight.Medium),
                    )
                }
                if (calendarOpen) {
                    RoomieInlineCalendar(selected = due, today = today, onPick = { due = it; calendarOpen = false })
                }
                if (due != null) {
                    Text(
                        "Remove Date",
                        style = RoomieType.pillButton.copy(color = Surface.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { due = null; calendarOpen = false }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallEditButton(text: String, onClick: () -> Unit, fill: Brush, textColor: Color, border: Color, enabled: Boolean = true) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .width(96.dp)
            .height(36.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.pillButton.copy(color = textColor, fontWeight = FontWeight.Medium))
    }
}

/** "Delete Note / Confirm". */
@Composable
fun DeleteNoteDialog(note: Note, onCancel: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(20.dp)
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(12.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.22f), spotColor = ShadowWarm.copy(alpha = 0.22f))
                .clip(shape)
                .background(CorkInner)
                .border(1.dp, Tan.copy(alpha = 0.45f), shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Delete Note?", style = RoomieType.titleEmpty.copy(fontSize = 24.sp, lineHeight = 28.sp, color = TextPrimary))
            Text("Are you sure? This can’t be undone.", style = RoomieType.input.copy(lineHeight = 20.sp, color = TextPrimary))
            Text("“${note.text}”", style = RoomieType.choreMeta.copy(color = TextPrimary), maxLines = 3)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val buttonShape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(buttonShape)
                        .background(Brush.horizontalGradient(listOf(GlassFaintStart, GlassFaintEnd)))
                        .border(1.dp, Color.White.copy(alpha = 0.85f), buttonShape)
                        .clickable(role = Role.Button, onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Cancel", style = RoomieType.pillButton.copy(fontWeight = FontWeight.Medium))
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(buttonShape)
                        .background(Danger)
                        .clickable(role = Role.Button, onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Delete", style = RoomieType.pillButton.copy(color = Surface, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}
