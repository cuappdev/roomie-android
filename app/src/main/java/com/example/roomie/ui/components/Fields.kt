package com.example.roomie.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CalendarDayEnd
import com.example.roomie.ui.theme.CalendarDayStart
import com.example.roomie.ui.theme.CalendarSelectedEnd
import com.example.roomie.ui.theme.CalendarSelectedStart
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Placeholder
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.fullLabel
import com.example.roomie.ui.util.label
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, style: TextStyle = RoomieType.fieldLabel) {
    Text(text, modifier = modifier, style = style)
}

/**
 * Bordered input from the design: white fill, 1dp tan border (2dp brown when focused),
 * 10dp corners, 14dp padding. Pass [height] = null for a multi-line text area.
 */
@Composable
fun RoomieTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp? = 50.dp,
    minHeight: Dp = 50.dp,
    corner: Dp = 10.dp,
    background: Color = Surface,
    textStyle: TextStyle = RoomieType.input,
    suffix: String? = null,
    suffixStyle: TextStyle = RoomieType.body.copy(color = TextSecondary),
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    contentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(corner)
    val borderColor = when {
        isError -> Danger
        focused -> BrandBrown
        else -> Border
    }
    val singleLine = height != null
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier.height(minHeight))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        textStyle = textStyle.copy(color = TextPrimary),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        cursorBrush = SolidColor(BrandBrown),
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(shape)
                    .background(background)
                    .border(if (focused || isError) 2.dp else 1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = Placeholder))
                    inner()
                }
                if (suffix != null) Text(suffix, style = suffixStyle)
            }
        },
    )
}

/** Label above a field, matching the 6dp label gap used throughout the design. */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = RoomieType.fieldLabel,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label, style = labelStyle)
        content()
    }
}

/** Calendar button that opens a date picker ("Tue, Nov 17  >"). */
@Composable
fun DateButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_calendar), null, Modifier.size(20.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.input, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right_18), null, Modifier.size(18.dp), tint = Color.Unspecified)
    }
}

/** Dropdown trigger ("Select a due date  v"); the chevron flips up while [expanded]. */
@Composable
fun DropdownField(
    text: String?,
    placeholder: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.05f), spotColor = ShadowWarm.copy(alpha = 0.05f))
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(role = Role.DropdownList, onClickLabel = contentDescription, onClick = onClick)
            .padding(start = 15.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text ?: placeholder, style = RoomieType.input.copy(lineHeight = 20.sp, color = if (text == null) Placeholder else TextPrimary), modifier = Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.ic_dropdown_chevron),
            null,
            Modifier.size(width = 12.dp, height = 6.dp).rotate(if (expanded) 0f else 180f),
            tint = Color.Unspecified,
        )
    }
}

private val CalendarDayFill = Brush.linearGradient(listOf(CalendarDayStart, CalendarDayEnd))
private val CalendarSelectedFill = Brush.linearGradient(listOf(CalendarSelectedStart, CalendarSelectedEnd))
private val WEEKDAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Inline month calendar from the "Leave a Note" design. Weeks start on Monday; days before [today]
 * are faded and can't be picked, and the trailing days of the next month fill out the last week.
 */
@Composable
fun RoomieInlineCalendar(selected: LocalDate?, today: LocalDate, onPick: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var month by remember { mutableStateOf(YearMonth.from(selected ?: today)) }
    val canGoBack = month.isAfter(YearMonth.from(today))
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.05f), spotColor = ShadowWarm.copy(alpha = 0.05f))
            .clip(shape)
            .background(Surface)
            .border(0.5.dp, Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(25.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(month.label(), style = RoomieType.input.copy(color = BrandBrown), modifier = Modifier.weight(1f))
            MonthArrow("‹", "Previous month", canGoBack) { month = month.minusMonths(1) }
            MonthArrow("›", "Next month", true) { month = month.plusMonths(1) }
        }
        Row(Modifier.fillMaxWidth().height(20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WEEKDAY_LETTERS.forEach { Text(it, style = RoomieType.rowMeta, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        val first = month.atDay(1)
        val lead = first.dayOfWeek.value - 1
        val weeks = (lead + month.lengthOfMonth() + 6) / 7
        repeat(weeks) { w ->
            Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { d ->
                    val offset = w * 7 + d - lead
                    if (offset < 0) {
                        Box(Modifier.weight(1f))
                    } else {
                        val day = first.plusDays(offset.toLong())
                        val isSelected = day == selected
                        val enabled = !day.isBefore(today)
                        val cellShape = RoundedCornerShape(7.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .alpha(if (enabled) 1f else 0.45f)
                                .clip(cellShape)
                                .background(if (isSelected) CalendarSelectedFill else CalendarDayFill)
                                .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onPick(day) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.dayOfMonth.toString(),
                                style = RoomieType.rowMeta.copy(color = if (isSelected) Surface else TextPrimary, lineHeight = 14.sp),
                                modifier = Modifier.semantics { contentDescription = day.fullLabel() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(width = 30.dp, height = 25.dp)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = RoomieType.input.copy(fontSize = 24.sp, lineHeight = 28.sp, color = if (enabled) BrandBrown else Tan))
    }
}

@Composable
fun ErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text != null) Text(text, modifier = modifier, style = RoomieType.caption.copy(color = Danger, fontSize = 13.sp))
}

@Composable
fun IconTextRow(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier, style: TextStyle = RoomieType.body) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = style)
    }
}
