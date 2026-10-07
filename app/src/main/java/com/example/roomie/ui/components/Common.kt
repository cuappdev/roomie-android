package com.example.roomie.ui.components

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.data.MemberStatus
import com.example.roomie.data.Roommate
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Scrim
import com.example.roomie.ui.theme.ShadowBrown
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.toUtcMillis
import com.example.roomie.ui.util.utcMillisToLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate

/** White tab header: brown title bottom-aligned with an optional trailing action. */
@Composable
fun RoomieHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Surface)
            .statusBarsPadding()
            .height(73.dp)
            .padding(start = 20.dp, end = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = RoomieType.screenTitle, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit) {
    IconAction(R.drawable.ic_settings, "Settings", onClick, Modifier.padding(bottom = 0.dp))
}

enum class RoomieTab(val label: String, val icon: Int, val activeIcon: Int) {
    Home("Home", R.drawable.ic_tab_home, R.drawable.ic_tab_home_active),
    Chores("Chores", R.drawable.ic_tab_chores, R.drawable.ic_tab_chores_active),
    Notes("Note board", R.drawable.ic_tab_notes, R.drawable.ic_tab_notes_active),
    Shopping("Shopping list", R.drawable.ic_tab_cart, R.drawable.ic_tab_cart_active),
}

/** Four icon-only tabs; each keeps an accessible name. */
@Composable
fun RoomieBottomBar(selected: RoomieTab?, onSelect: (RoomieTab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, clip = false, ambientColor = ShadowBrown.copy(alpha = 0.3f), spotColor = ShadowBrown.copy(alpha = 0.3f))
            .background(Surface, shape)
            .navigationBarsPadding()
            .padding(top = 4.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
    ) {
        RoomieTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (isSelected) tab.activeIcon else tab.icon),
                    contentDescription = tab.label,
                    modifier = Modifier.size(30.dp),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Composable
fun BuddyAvatar(buddy: Buddy, size: Dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(painterResource(buddy.drawable), contentDescription, modifier.size(size))
}

@Composable
fun StatusPill(text: String, background: Color, contentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = RoomieType.pill.copy(color = contentColor))
    }
}

@Composable
fun MemberStatusPill(status: MemberStatus, ownerStyle: Boolean = true, pendingLabel: String = "Pending") {
    when (status) {
        MemberStatus.Owner -> if (ownerStyle) StatusPill("Owner", NoteGreen, SuccessText) else StatusPill("Owner", PeachPill, TextSecondary)
        MemberStatus.Joined -> StatusPill("Joined", NoteGreen, SuccessText)
        MemberStatus.InviteSent -> StatusPill(pendingLabel, PeachPill, TextSecondary)
    }
}

/** 24dp rounded checkbox with a 44dp touch target. */
@Composable
fun RoomieCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(shape)
                .background(if (checked) BrandBrown else Color.Transparent)
                .border(2.dp, BrandBrown, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check_small), null, Modifier.size(14.dp), tint = Surface)
        }
    }
}

/** Day pill + chore pill pair used on Home ("This Week's Tasks") and Chores ("Upcoming"). */
@Composable
fun DayTaskRow(day: String, task: String, who: String, modifier: Modifier = Modifier, height: Dp = 36.dp, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(6.dp)
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Wide enough for "Wednesday" on one line; grows rather than wraps at large font scales.
        Box(Modifier.widthIn(min = 100.dp).clip(shape).background(PeachPill).height(height).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(day, style = RoomieType.rowTitle, maxLines = 1, softWrap = false)
        }
        Row(
            Modifier
                .weight(1f)
                .height(height)
                .clip(shape)
                .background(PeachPill)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(task, style = RoomieType.rowTitle, modifier = Modifier.weight(1f), maxLines = 1)
            Text(who, style = RoomieType.rowMeta)
        }
    }
}

/** Roommate row with avatar, name and an optional status pill. */
@Composable
fun RoommateRow(roommate: Roommate, isMe: Boolean, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BuddyAvatar(roommate.buddy, 32.dp)
        Text(if (isMe) "${roommate.firstName} (you)" else roommate.firstName, style = RoomieType.listTitle, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Peach invite code card with a Copy button; also offers Android share for the invite link. */
@Composable
fun InviteCodeCard(code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PeachPill)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Invite code", style = RoomieType.rowMeta.copy(fontWeight = FontWeight.Medium))
            Text(code, style = RoomieType.inviteCode)
        }
        Row(
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Surface)
                .clickable(role = Role.Button, onClickLabel = "Copy invite code") {
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Roomie invite code", code)))
                        Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_copy), null, Modifier.size(16.dp), tint = Color.Unspecified)
            Text("Copy", style = RoomieType.linkButton.copy(fontSize = 14.sp))
        }
    }
}

@Composable
fun ShareInviteButton(houseName: String, code: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    SecondaryButton(
        text = "Share invite link",
        icon = R.drawable.ic_link,
        modifier = modifier,
        onClick = {
            val text = "Join ${houseName.ifBlank { "our house" }} on Roomie with invite code $code\nhttps://roomie.app/join/$code"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, "Share invite link"))
        },
    )
}

/** White bordered card container used on Me, Chores and Shopping. */
@Composable
fun RoomieCard(modifier: Modifier = Modifier, corner: Dp = 8.dp, borderColor: Color = com.example.roomie.ui.theme.Divider, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(corner)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface)
            .border(1.dp, borderColor, shape),
        content = content,
    )
}

/** Bottom sheet with the design's cream surface, drag handle, title and a text action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomieBottomSheet(
    title: String,
    actionLabel: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        containerColor = Background,
        scrimColor = Scrim.copy(alpha = 0.45f),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp).size(width = 40.dp, height = 5.dp).clip(CircleShape).background(Border))
        },
    ) {
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = RoomieType.sheetTitle, modifier = Modifier.weight(1f))
                TextAction(actionLabel, close)
            }
            content()
        }
    }
}

/** Material date picker restricted to today or later. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomieDatePickerDialog(initial: LocalDate, today: LocalDate, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !utcTimeMillis.utcMillisToLocalDate().isBefore(today)
        },
    )
    val colors = DatePickerDefaults.colors(containerColor = Background)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextAction("Done", {
                state.selectedDateMillis?.let { onPicked(it.utcMillisToLocalDate()) }
                onDismiss()
            })
        },
        dismissButton = { TextAction("Cancel", onDismiss) },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors)
    }
}
