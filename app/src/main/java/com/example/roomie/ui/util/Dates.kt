package com.example.roomie.ui.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "Tue, Nov 17" */
fun LocalDate.shortLabel(): String = format(shortDate)

/** "Nov 17" */
fun LocalDate.monthDayLabel(): String = format(monthDay)

fun LocalDate.dayName(): String = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)

fun LocalDate.monthAbbrev(): String = month.getDisplayName(TextStyle.SHORT, Locale.US)

/** Day label for chore rows: Tonight / Tomorrow / weekday this week / short date further out. */
fun LocalDate.choreDayLabel(today: LocalDate, todayLabel: String = "Today"): String {
    val days = ChronoUnit.DAYS.between(today, this)
    return when {
        days == 0L -> todayLabel
        days == 1L -> "Tomorrow"
        days in 2..6 -> dayName()
        else -> monthDayLabel()
    }
}

/** "2 days ago", "last month", "just now" */
fun LocalDateTime.agoLabel(now: LocalDateTime): String {
    val d = Duration.between(this, now)
    val days = d.toDays()
    return when {
        d.toMinutes() < 1 -> "just now"
        d.toHours() < 1 -> "${d.toMinutes()} min ago"
        days < 1 -> "today"
        days == 1L -> "yesterday"
        days < 21 -> "$days days ago"
        days < 60 -> "last month"
        else -> "${days / 30} months ago"
    }
}

/** Material date pickers work in UTC millis. */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

fun Long.utcMillisToLocalDate(): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneOffset.UTC).toLocalDate()
