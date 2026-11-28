package com.example.roomie.ui.notes.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.ui.components.WhiteBorderWidth
import com.example.roomie.ui.notes.components.StickyNote
import com.example.roomie.ui.notes.components.StickySize
import com.example.roomie.ui.notes.components.TackView
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import kotlin.math.hypot

enum class HoldAction(val icon: Int, @StringRes val title: Int, @StringRes val hint: Int) {
    Edit(R.drawable.ic_hold_edit, R.string.hold_edit, R.string.hold_release_edit),
    Delete(R.drawable.ic_hold_delete, R.string.hold_delete, R.string.hold_release_delete),
    Pin(R.drawable.ic_hold_pin, R.string.hold_pin, R.string.hold_release_pin),
}

/** What a held note offers: your own notes can be edited and deleted; anyone's can be pinned. */
fun holdActionsFor(isMine: Boolean): List<HoldAction> =
    if (isMine) listOf(HoldAction.Edit, HoldAction.Delete, HoldAction.Pin) else listOf(HoldAction.Pin)

internal val HoldScrim = Color(0xD60A0908)

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

private val HOLD_SLOTS = mapOf(
    HoldAction.Edit to Offset(-48f, -7f),
    HoldAction.Delete to Offset(-62f, 65f),
    HoldAction.Pin to Offset(-28f, 141f),
)

/** Action button centres around a held note, fanned out on whichever side has room. */
fun holdTargetsPx(
    bounds: Rect,
    overlayWidth: Float,
    actions: List<HoldAction>,
    density: Float,
): Map<HoldAction, Offset> {
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
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClickLabel = stringResource(R.string.cancel),
                    onClick = onDismiss,
                ),
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
                .graphicsLayer {
                    scaleX = 1.06f
                    scaleY = 1.06f
                },
        )
        targets.forEach { (action, center) ->
            val active = action == highlighted
            val size = if (active) 64.dp else 56.dp
            val half = with(density) { (size / 2).toPx() }
            Box(
                Modifier
                    .offset { IntOffset((center.x - half).toInt(), (center.y - half).toInt()) }
                    .size(size)
                    .shadow(
                        6.dp,
                        CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.24f),
                        spotColor = Color.Black.copy(alpha = 0.24f),
                    )
                    .clip(CircleShape)
                    .background(if (active) Surface else BrandBrown)
                    .border(WhiteBorderWidth, Color.White.copy(alpha = if (active) 1f else 0.24f), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = action.label(pinned)) { onPick(action) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(action.icon),
                    action.label(pinned),
                    Modifier.size(if (active) 27.dp else 24.dp),
                    tint = if (active) BrandBrown else Surface,
                )
            }
        }
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                highlighted?.label(pinned) ?: stringResource(R.string.hold_choose),
                style = RoomieType.greeting.copy(color = Surface),
            )
            Text(
                when {
                    highlighted == HoldAction.Pin && pinned -> stringResource(R.string.hold_release_unpin)
                    highlighted != null -> stringResource(highlighted.hint)
                    targets.size == 1 -> stringResource(R.string.hold_slide_pin)
                    else -> stringResource(R.string.hold_slide_all)
                },
                style = RoomieType.input.copy(color = Surface.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
private fun HoldAction.label(pinned: Boolean) =
    stringResource(if (this == HoldAction.Pin && pinned) R.string.hold_unpin else title)
