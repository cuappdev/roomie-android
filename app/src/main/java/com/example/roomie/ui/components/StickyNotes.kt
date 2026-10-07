package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.Note
import com.example.roomie.data.NoteColor
import com.example.roomie.data.Tack
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.Inter
import com.example.roomie.ui.theme.NoteBlue
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.NotePeach
import com.example.roomie.ui.theme.NotePink
import com.example.roomie.ui.theme.NotePurple
import com.example.roomie.ui.theme.NoteYellow
import com.example.roomie.ui.theme.Poppins
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowBrown
import com.example.roomie.ui.theme.StickyText
import com.example.roomie.ui.theme.TackBlue
import com.example.roomie.ui.theme.TackGreen
import com.example.roomie.ui.theme.TackRed
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.util.monthDayLabel
import com.example.roomie.ui.util.shortLabel

fun NoteColor.color(): Color = when (this) {
    NoteColor.Yellow -> NoteYellow
    NoteColor.Peach -> NotePeach
    NoteColor.Green -> NoteGreen
    NoteColor.Pink -> NotePink
    NoteColor.Purple -> NotePurple
    NoteColor.Blue -> NoteBlue
}

/** The three sticky sizes in the design: Home preview, dense board (3 columns) and roomy board (2 columns). */
enum class StickySize(val width: Dp, val height: Dp, val fontSize: Float, val lineHeight: Float, val authorLineHeight: Float) {
    Small(96.dp, 84.dp, 11f, 14f, 13.2f),
    Medium(96.dp, 106.dp, 11.5f, 14.72f, 13.8f),
    Large(140.dp, 120.dp, 14f, 17.92f, 16.8f),
}

// Slight hand-placed tilts, cycled through so the board looks pinned up by people.
private val SMALL_TILTS = listOf(-4f, 5f, -3f, 4f, -5f)
private val MEDIUM_TILTS = listOf(-3f, 5f, -2f, 3f, -4f, 4f, -3f, 5f, -2f, 3f, -4f, 4f)
private val LARGE_TILTS = listOf(-3f, 4f, 3f, -3f, -4f, 4f)

fun tiltFor(size: StickySize, index: Int): Float = when (size) {
    StickySize.Small -> SMALL_TILTS[index % SMALL_TILTS.size]
    StickySize.Medium -> MEDIUM_TILTS[index % MEDIUM_TILTS.size]
    StickySize.Large -> LARGE_TILTS[index % LARGE_TILTS.size]
}

@Composable
fun StickyNote(
    note: Note,
    authorName: String,
    pinned: Boolean,
    size: StickySize,
    tilt: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDueDate: Boolean = true,
    removable: Boolean = false,
    onRemove: () -> Unit = {},
) {
    val text = TextStyle(fontFamily = Poppins, fontSize = size.fontSize.sp, lineHeight = size.lineHeight.sp, color = StickyText)
    val description = buildString {
        append(note.text).append(", by ").append(authorName)
        if (pinned) append(", pinned")
        note.dueDate?.let { append(", due ").append(it.shortLabel()) }
    }
    Box(
        modifier
            .rotate(tilt)
            .size(size.width, size.height)
            .shadow(3.dp, RectangleShape, clip = false, ambientColor = ShadowBrown, spotColor = ShadowBrown)
            .background(note.color.color())
            .clickable(role = Role.Button, onClickLabel = "Read note", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Column(Modifier.fillMaxSize().padding(start = 9.dp, end = 9.dp, top = 13.dp, bottom = 7.dp)) {
            Text(note.text, style = text, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val due = note.dueDate
            if (showDueDate && due != null) {
                Spacer(Modifier.size(5.dp))
                DueChip(if (size == StickySize.Large) due.shortLabel() else due.monthDayLabel())
            }
            Spacer(Modifier.weight(1f))
            Text(
                "- $authorName",
                style = text.copy(lineHeight = size.authorLineHeight.sp),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                maxLines = 1,
            )
        }
        TackView(note.tack, Modifier.align(Alignment.TopCenter))
        if (pinned) PinnedBadge(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp))
        if (removable) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f))
                    .clickable(role = Role.Button, onClickLabel = "Remove note", onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_trash), "Remove note", Modifier.size(14.dp), tint = Color.Unspecified)
            }
        }
    }
}

@Composable
private fun DueChip(label: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_calendar_tiny), null, Modifier.size(10.dp), tint = Color.Unspecified)
        Text(label, style = RoomieType.dueChip)
    }
}

@Composable
private fun PinnedBadge(modifier: Modifier) {
    Box(
        modifier.size(18.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_pinned_badge), "Pinned", Modifier.size(11.dp), tint = Color.Unspecified)
    }
}

@Composable
fun TackView(tack: Tack, modifier: Modifier = Modifier) {
    when (tack) {
        Tack.StarOrange, Tack.StarPink -> Icon(
            painterResource(if (tack == Tack.StarOrange) R.drawable.ic_tack_star_orange else R.drawable.ic_tack_star_pink),
            null,
            modifier.offset(y = (-7).dp).size(15.dp),
            tint = Color.Unspecified,
        )
        else -> Box(
            modifier
                .offset(y = (-5).dp)
                .size(12.dp)
                .shadow(1.dp, CircleShape)
                .background(
                    when (tack) {
                        Tack.DotGreen -> TackGreen
                        Tack.DotRed -> TackRed
                        else -> TackBlue
                    },
                    CircleShape,
                ),
        )
    }
}

/** Tan-framed cork board. The edit button sits in the top-right corner, as in the design. */
@Composable
fun CorkBoard(
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    editing: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CorkInner)
            .border(12.dp, Tan, shape)
            .padding(12.dp),
    ) {
        content()
        if (onEdit != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (editing) BrandBrown else CorkInner)
                    .border(1.dp, BrandBrown, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = if (editing) "Done editing board" else "Edit board", onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    if (editing) "Done editing board" else "Edit board",
                    Modifier.size(16.dp),
                    tint = if (editing) Color.White else Color.Unspecified,
                )
            }
        }
    }
}

/** "+N more" stack on Home that opens the full board. */
@Composable
fun MoreNotesStack(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 84.dp, height = 78.dp)) {
        Box(Modifier.matchParentSizeOffset(3.dp, 2.dp).rotate(5f).shadow(2.dp, RectangleShape, clip = false).background(NotePurple))
        Box(Modifier.matchParentSizeOffset(0.dp, 0.dp).rotate(-3f).shadow(2.dp, RectangleShape, clip = false).background(NoteBlue))
        Column(
            Modifier
                .size(width = 84.dp, height = 78.dp)
                .shadow(3.dp, RectangleShape, clip = false, ambientColor = ShadowBrown, spotColor = ShadowBrown)
                .background(NotePeach)
                .clickable(role = Role.Button, onClickLabel = "See $count more notes", onClick = onClick),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("+$count", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 1.em, color = BrandBrown))
            Text("more", style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandBrown))
        }
    }
}

private fun Modifier.matchParentSizeOffset(x: Dp, y: Dp): Modifier = this.offset(x, y).size(width = 84.dp, height = 78.dp)

/** Full note, opened by tapping a sticky (stickies cut off after 3 lines). */
@Composable
fun NoteDetailDialog(
    note: Note,
    authorName: String,
    pinned: Boolean,
    canPin: Boolean,
    isMine: Boolean,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = note.color.color(),
        title = null,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(note.text, style = TextStyle(fontFamily = Poppins, fontSize = 18.sp, lineHeight = 24.sp, color = StickyText))
                note.dueDate?.let { DueChip(it.shortLabel()) }
                Text("- $authorName", style = TextStyle(fontFamily = Poppins, fontSize = 15.sp, color = StickyText), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
            }
        },
        confirmButton = { TextAction("Close", onDismiss) },
        dismissButton = {
            Row {
                if (pinned || canPin) TextAction(if (pinned) "Unpin" else "Pin for 7 days", onTogglePin)
                if (isMine) TextAction("Delete", onDelete, style = RoomieType.linkButton.copy(color = com.example.roomie.ui.theme.Danger))
            }
        },
    )
}
