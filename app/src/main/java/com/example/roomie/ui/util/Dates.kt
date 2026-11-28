package com.example.roomie.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.roomie.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Date formats follow the phone's language; the patterns keep the design's order and length.
private fun pattern(p: String) = DateTimeFormatter.ofPattern(p, Locale.getDefault())

/** "Tue, Nov 17" */
fun LocalDate.shortLabel(): String = format(pattern("EEE, MMM d"))

/** "Thu, Nov 19, 2026" */
fun LocalDate.fullLabel(): String = format(pattern("EEE, MMM d, yyyy"))

/** "Thursday, November 19" */
fun LocalDate.longLabel(): String = format(pattern("EEEE, MMMM d"))

/** "Week of Nov 16, 2026" */
@Composable
fun LocalDate.weekOfLabel(): String = stringResource(R.string.week_of, format(pattern("MMM d, yyyy")))

/** "November 16 – 22", or "Nov 30 – Dec 6" across months. [short] always abbreviates the month. */
fun LocalDate.weekRangeLabel(short: Boolean = false): String {
    val end = plusDays(6)
    return when {
        month != end.month -> "${monthDayLabel()} – ${end.monthDayLabel()}"
        short -> "${monthDayLabel()} – ${end.dayOfMonth}"
        else -> "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $dayOfMonth – ${end.dayOfMonth}"
    }
}

/** "November 2026" */
fun YearMonth.label(): String = format(pattern("MMMM yyyy"))

/** "Nov 17" */
fun LocalDate.monthDayLabel(): String = format(pattern("MMM d"))

fun LocalDate.dayName(): String = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

fun LocalDate.monthAbbrev(): String = month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** "2 days ago", "last month", "just now" */
@Composable
fun LocalDateTime.agoLabel(now: LocalDateTime): String {
    val d = Duration.between(this, now)
    val days = d.toDays().toInt()
    return when {
        d.toMinutes() < 1 -> stringResource(R.string.ago_just_now)
        d.toHours() < 1 -> d.toMinutes().toInt().let { pluralStringResource(R.plurals.ago_minutes, it, it) }
        days < 1 -> stringResource(R.string.ago_today)
        days == 1 -> stringResource(R.string.ago_yesterday)
        days < 21 -> pluralStringResource(R.plurals.ago_days, days, days)
        days < 60 -> stringResource(R.string.ago_last_month)
        else -> (days / 30).let { pluralStringResource(R.plurals.ago_months, it, it) }
    }
}
