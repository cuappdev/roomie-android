package com.example.roomie.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.data.Chore
import com.example.roomie.data.Roommate
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CorkInner
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.DoneCardStart
import com.example.roomie.ui.theme.GlassFaintEnd
import com.example.roomie.ui.theme.GlassFaintStart
import com.example.roomie.ui.theme.HeaderCardBorder
import com.example.roomie.ui.theme.NoteGreen
import com.example.roomie.ui.theme.NotePink
import com.example.roomie.ui.theme.NotePurple
import com.example.roomie.ui.theme.NoteYellow
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SectionBorder
import com.example.roomie.ui.theme.SectionEnd
import com.example.roomie.ui.theme.SectionStart
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TanButtonEnd
import com.example.roomie.ui.theme.TanButtonStart
import com.example.roomie.ui.theme.TanButtonText
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.monthDayLabel
import com.example.roomie.ui.util.shortLabel
import java.time.LocalDate

private val GlassFaint = Brush.horizontalGradient(listOf(GlassFaintStart, GlassFaintEnd))

/** Fill behind a roommate's person tag, matching their avatar's background. */
fun Buddy.tagColor(): Color = when (this) {
    Buddy.Mug -> PeachPill
    Buddy.Sock -> NoteYellow
    Buddy.Plant -> NotePurple
    Buddy.Toaster -> NoteGreen
    Buddy.Teapot -> NotePink
}

/** "Mina", "Mina and Leah", "Mina, Leah and Stef". */
fun List<Roommate>.namesLabel(): String = when (size) {
    0 -> "Unassigned"
    1 -> first().firstName
    else -> dropLast(1).joinToString(", ") { it.firstName } + " and " + last().firstName
}

/** "Chores / Card": title with a completion circle, assignees, due line and the rotation badge. */
@Composable
fun ChoreCard(chore: Chore, assignees: List<Roommate>, today: LocalDate, onToggle: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.1f), spotColor = ShadowWarm.copy(alpha = 0.1f))
            .clip(shape)
            .background(if (chore.done) Brush.horizontalGradient(listOf(DoneCardStart, Surface)) else SolidColor(Surface))
            .border(0.7.dp, if (chore.done) NoteGreen else Border, shape)
            .clickable(onClickLabel = "Open ${chore.title}", onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(chore.title, style = RoomieType.choreTitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                painterResource(if (chore.done) R.drawable.ic_chore_done else R.drawable.ic_chore_pending),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .toggleable(value = chore.done, role = Role.Checkbox, onValueChange = { onToggle() })
                    .semantics { contentDescription = "${chore.title} done" },
                tint = Color.Unspecified,
            )
        }
        if (assignees.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().height(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    assignees.forEach { BuddyAvatar(it.buddy, 24.dp) }
                }
                Text(assignees.namesLabel(), style = RoomieType.choreMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().height(18.dp), verticalAlignment = Alignment.CenterVertically) {
            val overdue = !chore.done && chore.dueDate.isBefore(today)
            Text(
                when {
                    chore.done -> "Completed · ${chore.completedOn!!.monthDayLabel()}"
                    chore.dueDate == today -> "Due Today"
                    overdue -> "Overdue · ${chore.dueDate.shortLabel()}"
                    else -> "Due ${chore.dueDate.shortLabel()}"
                },
                style = RoomieType.choreMeta.copy(color = if (overdue) Danger else TextSecondary),
                modifier = Modifier.weight(1f).wrapContentHeight(Alignment.CenterVertically),
            )
            if (chore.rotationId != null) RotationBadge()
        }
    }
}

@Composable
fun RotationBadge(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_repeat), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text("Weekly rotation", style = RoomieType.smallLink)
    }
}

/** "‹ Back to chores" link at the top of the chore sub-pages. */
@Composable
fun BackLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_chevron_left_small), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.smallLink.copy(fontWeight = FontWeight.Normal))
    }
}

/** Light tan gradient button ("View Rotation", "View & Edit Rotation"). */
@Composable
fun TanButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.12f), spotColor = ShadowWarm.copy(alpha = 0.12f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(TanButtonStart, TanButtonEnd)))
            .border(1.dp, BrandBrown.copy(alpha = 0.6f), shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.glassButton.copy(color = TanButtonText))
    }
}

/** Brown gradient card that heads the rotation pages. */
@Composable
fun BrownHeaderCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.15f), spotColor = ShadowWarm.copy(alpha = 0.15f))
            .clip(shape)
            .background(BrandGradient)
            .border(0.6.dp, HeaderCardBorder, shape)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = RoomieType.sheetTitle.copy(fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = Surface))
        Text(subtitle, style = RoomieType.choreMeta.copy(color = Background))
    }
}

/** Translucent form section with a brown title ("Chore Details", "Assignment", "People & Schedule"). */
@Composable
fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.08f), spotColor = ShadowWarm.copy(alpha = 0.08f))
            .clip(shape)
            .background(Background)
            .background(Brush.horizontalGradient(listOf(SectionStart, SectionEnd)))
            .border(1.dp, SectionBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = RoomieType.sectionTitle.copy(color = BrandBrown, lineHeight = 20.sp))
        content()
    }
}

/** 12sp label above a chores form control. */
@Composable
fun ChoreField(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = RoomieType.onboardingLabel)
        content()
    }
}

/** Faint tan dropdown trigger used throughout the chores forms. */
@Composable
fun GlassDropdownTrigger(
    expanded: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    startPadding: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.05f), spotColor = ShadowWarm.copy(alpha = 0.05f))
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(0.8.dp, Color.White.copy(alpha = 0.82f), shape)
            .clickable(role = Role.DropdownList, onClickLabel = contentDescription, onClick = onClick)
            .padding(start = startPadding, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) { content() }
        Icon(
            painterResource(if (expanded) R.drawable.ic_chevron_up_small else R.drawable.ic_chevron_down_small),
            null,
            Modifier.size(16.dp),
            tint = Color.Unspecified,
        )
    }
}

/** White options panel that opens under a trigger. */
@Composable
fun DropdownOptions(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(5.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.12f), spotColor = ShadowWarm.copy(alpha = 0.12f))
            .clip(shape)
            .background(Surface)
            .border(0.65.dp, Border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Single-choice dropdown ("1 person", "Week of Nov 16, 2026"). Collapses once something is picked. */
@Composable
fun <T> ChoiceDropdown(
    value: T?,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    placeholder: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassDropdownTrigger(open, { open = !open }, contentDescription) {
            Text(
                value?.let(label) ?: placeholder,
                style = RoomieType.choreMeta.copy(color = if (value == null) TextSecondary else TextPrimary),
            )
        }
        if (open) {
            DropdownOptions {
                options.forEach { option ->
                    val selected = option == value
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) PeachPill else Color.Transparent)
                            .selectable(selected = selected, role = Role.RadioButton) { onSelect(option); open = false }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label(option), style = RoomieType.choreMeta.copy(color = TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
                    }
                }
            }
        }
    }
}

/** "Roomie / Person tag": avatar and first name on the roommate's colour. */
@Composable
fun PersonTag(roommate: Roommate, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier
            .clip(shape)
            .background(roommate.buddy.tagColor())
            .border(1.dp, Tan.copy(alpha = 0.65f), shape)
            .padding(start = 4.dp, end = 7.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyAvatar(roommate.buddy, 18.dp)
        Text(roommate.firstName, style = RoomieType.rowMeta.copy(color = TextPrimary, fontWeight = FontWeight.Medium))
    }
}

/**
 * Multi-select roommate picker ("People Assigned", "People to Rotate"). The trigger shows the
 * picked people as tags; the panel has an Everyone shortcut and a Done Selecting button.
 */
@Composable
fun PeoplePicker(
    members: List<Roommate>,
    selectedIds: List<String>,
    onChange: (List<String>) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = members.filter { it.id in selectedIds }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassDropdownTrigger(open, { open = !open }, contentDescription, startPadding = 8.dp) {
            if (selected.isEmpty()) {
                Text("Select people", style = RoomieType.choreMeta, modifier = Modifier.padding(start = 6.dp))
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    selected.forEach { PersonTag(it) }
                }
            }
        }
        if (open) {
            DropdownOptions {
                val everyone = selected.size == members.size
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .toggleable(value = everyone, role = Role.Checkbox) { onChange(if (it) members.map { m -> m.id } else emptyList()) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Everyone", style = RoomieType.onboardingLabel.copy(color = TextPrimary), modifier = Modifier.weight(1f))
                    SoftCheckbox(everyone)
                }
                members.forEach { m ->
                    val checked = m.id in selectedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .toggleable(value = checked, role = Role.Checkbox) {
                                // Keep house order so rotations go round the house the same way every time.
                                val next = if (it) selectedIds + m.id else selectedIds - m.id
                                onChange(members.map { r -> r.id }.filter { id -> id in next })
                            }
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BuddyAvatar(m.buddy, 22.dp)
                        Text(m.firstName, style = RoomieType.choreMeta.copy(color = TextPrimary), modifier = Modifier.weight(1f))
                        SoftCheckbox(checked)
                    }
                }
                Spacer(Modifier.height(6.dp))
                val shape = RoundedCornerShape(10.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(shape)
                        .background(GlassFaint)
                        .border(1.dp, Tan.copy(alpha = 0.5f), shape)
                        .clickable(role = Role.Button) { open = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Done Selecting", style = RoomieType.smallLink)
                }
            }
        }
    }
}

/** "Roomie / Checkbox": 18dp, soft green with a check when ticked. Visual only. */
@Composable
fun SoftCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier
            .size(18.dp)
            .clip(shape)
            .background(if (checked) NoteGreen else CorkInner)
            .border(if (checked) 1.dp else 1.2.dp, if (checked) SuccessText else Tan, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(painterResource(R.drawable.ic_check_green), null, Modifier.size(9.75.dp), tint = Color.Unspecified)
    }
}

/** Two- or three-way pill switch ("My Chores / All Chores", "Assign Once / Rotate Weekly"). */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp,
    corner: Dp = 12.dp,
    innerCorner: Dp = 8.dp,
) {
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.06f), spotColor = ShadowWarm.copy(alpha = 0.06f))
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(1.dp, Color.White.copy(alpha = 0.85f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, option ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .height(height - 8.dp)
                    .clip(RoundedCornerShape(innerCorner))
                    .then(if (selected) Modifier.background(BrandGradient) else Modifier)
                    .selectable(selected = selected, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    style = RoomieType.rowMeta.copy(
                        color = if (selected) Surface else BrandBrown,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

/** "Rotation Preview": who has the chore for each of the listed weeks. */
@Composable
fun RotationPreview(weeks: List<Pair<LocalDate, List<Roommate>>>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(4.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.1f), spotColor = ShadowWarm.copy(alpha = 0.1f))
            .clip(shape)
            .background(Surface)
            .border(0.7.dp, Border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Rotation Preview", style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Week of", style = RoomieType.choreMeta, modifier = Modifier.width(62.dp))
            Text("Assigned to", style = RoomieType.choreMeta)
        }
        weeks.forEach { (week, people) ->
            Row(Modifier.heightIn(min = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(week.monthDayLabel(), style = RoomieType.choreMeta.copy(color = TextPrimary, fontWeight = FontWeight.Medium), modifier = Modifier.width(62.dp))
                people.forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        BuddyAvatar(p.buddy, 20.dp)
                        Text(p.firstName, style = RoomieType.choreMeta)
                    }
                }
            }
        }
    }
}

/** Small glass button with brown text ("Manage Rotations"). */
@Composable
fun SmallGlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .height(34.dp)
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.07f), spotColor = ShadowWarm.copy(alpha = 0.07f))
            .clip(shape)
            .background(Background)
            .background(GlassFaint)
            .border(1.dp, Tan.copy(alpha = 0.55f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.smallLink.copy(lineHeight = 18.sp))
    }
}
