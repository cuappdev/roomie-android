package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.ChoreRules
import com.example.roomie.ui.components.RoomieInlineCalendar
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.CalendarDayEnd
import com.example.roomie.ui.theme.CalendarDayStart
import com.example.roomie.ui.theme.CalendarSelectedEnd
import com.example.roomie.ui.theme.CalendarSelectedStart
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.weekRangeLabel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val DayFill = Brush.linearGradient(listOf(CalendarDayStart, CalendarDayEnd))
private val TodayFill = Brush.linearGradient(listOf(CalendarSelectedStart, CalendarSelectedEnd))

/** "Chores / Week picker": compact week strip that expands to a month; tapping a date picks its week. */
@Composable
internal fun WeekPicker(
    week: LocalDate,
    today: LocalDate,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPickDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                3.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.08f),
                spotColor = ShadowWarm.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(Surface)
            .border(0.7.dp, Border, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) {
                        stringResource(
                            R.string.cd_chores_show_week,
                        )
                    } else {
                        stringResource(R.string.cd_chores_show_month)
                    },
                    onClick = onToggleExpanded,
                )
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                week.weekRangeLabel(),
                style = RoomieType.input.copy(fontWeight = FontWeight.Medium, color = BrandBrown, lineHeight = 20.sp),
                modifier = Modifier.weight(1f),
            )
            Icon(
                painterResource(if (expanded) R.drawable.ic_chevron_up_small else R.drawable.ic_chevron_down_small),
                null,
                Modifier.size(16.dp),
                tint = Color.Unspecified,
            )
        }
        if (expanded) {
            RoomieInlineCalendar(
                selected = today,
                today = today,
                onPick = {
                    onPickDate(it)
                    onToggleExpanded()
                },
                allowPast = true,
                framed = false,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ChoreRules.weekDays(week).forEach { day ->
                    val isToday = day == today
                    Column(
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) TodayFill else DayFill),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            style = RoomieType.rowMeta.copy(
                                color = if (isToday) Surface else TextSecondary,
                                lineHeight = 16.sp,
                            ),
                        )
                        Text(
                            day.dayOfMonth.toString(),
                            style = RoomieType.input.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (isToday) Surface else TextPrimary,
                                lineHeight = 20.sp,
                            ),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WeekStep(
                    stringResource(R.string.chores_week_previous),
                    R.drawable.ic_chevron_left_small,
                    iconFirst = true,
                    onClick = onPrevious,
                )
                Box(Modifier.weight(1f))
                WeekStep(
                    stringResource(R.string.chores_week_next),
                    R.drawable.ic_chevron_right_small,
                    iconFirst = false,
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
private fun WeekStep(text: String, icon: Int, iconFirst: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconFirst) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.smallLink.copy(fontWeight = FontWeight.Normal))
        if (!iconFirst) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
    }
}

@Preview
@Composable
private fun WeekPickerPreview() = RoomiePreview(padding = 16.dp) {
    val today = LocalDate.now()
    WeekPicker(ChoreRules.weekStart(today), today, expanded = false, onToggleExpanded = {
    }, onPickDate = {}, onPrevious = {}, onNext = {})
}

@Preview
@Composable
private fun WeekPickerExpandedPreview() = RoomiePreview(padding = 16.dp) {
    val today = LocalDate.now()
    WeekPicker(ChoreRules.weekStart(today), today, expanded = true, onToggleExpanded = {
    }, onPickDate = {}, onPrevious = {}, onNext = {})
}
