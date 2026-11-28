package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.notes.components.color
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CalendarDayEnd
import com.example.roomie.ui.theme.CalendarDayStart
import com.example.roomie.ui.theme.CalendarSelectedEnd
import com.example.roomie.ui.theme.CalendarSelectedStart
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.Tan
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.fullLabel
import com.example.roomie.ui.util.label
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val CalendarDayFill = Brush.linearGradient(listOf(CalendarDayStart, CalendarDayEnd))

private val WEEK = DayOfWeek.entries

private val CalendarSelectedFill = Brush.linearGradient(listOf(CalendarSelectedStart, CalendarSelectedEnd))

/**
 * Inline month calendar from the "Leave a Note" design. Weeks start on Monday; days before [today]
 * are faded and can't be picked unless [allowPast], and the trailing days of the next month fill out
 * the last week. [framed] = false drops the card so it can sit inside another one (the chores week picker).
 */
@Composable
fun RoomieInlineCalendar(
    selected: LocalDate?,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    allowPast: Boolean = false,
    framed: Boolean = true,
) {
    var month by remember { mutableStateOf(YearMonth.from(selected ?: today)) }
    val canGoBack = allowPast || month.isAfter(YearMonth.from(today))
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier
                        .shadow(
                            2.dp,
                            shape,
                            ambientColor = ShadowWarm.copy(alpha = 0.05f),
                            spotColor = ShadowWarm.copy(alpha = 0.05f),
                        )
                        .clip(shape)
                        .background(Surface)
                        .border(0.5.dp, Border, shape)
                        .padding(16.dp)
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(25.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(month.label(), style = RoomieType.input.copy(color = BrandBrown), modifier = Modifier.weight(1f))
            MonthArrow("‹", stringResource(R.string.calendar_previous_month), canGoBack) {
                month = month.minusMonths(1)
            }
            MonthArrow("›", stringResource(R.string.calendar_next_month), true) { month = month.plusMonths(1) }
        }
        Row(Modifier.fillMaxWidth().height(20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WEEK.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) }.forEach {
                Text(it, style = RoomieType.rowMeta, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
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
                        val enabled = allowPast || !day.isBefore(today)
                        val cellShape = RoundedCornerShape(7.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .alpha(if (enabled) 1f else 0.45f)
                                .clip(cellShape)
                                .background(if (isSelected) CalendarSelectedFill else CalendarDayFill)
                                .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) {
                                    onPick(day)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.dayOfMonth.toString(),
                                style = RoomieType.rowMeta.copy(
                                    color = if (isSelected) Surface else TextPrimary,
                                    lineHeight = 14.sp,
                                ),
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
        Text(
            glyph,
            style = RoomieType.input.copy(
                fontSize = 24.sp,
                lineHeight = 28.sp,
                color = if (enabled) BrandBrown else Tan,
            ),
        )
    }
}
