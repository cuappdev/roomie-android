package com.example.roomie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NoteRulesTest {
    private val today = LocalDate.of(2026, 11, 11)

    private fun note(
        id: String,
        daysAgo: Long,
        due: LocalDate? = null,
        pinnedUntil: LocalDate? = null,
    ) = Note(
        id = id,
        text = id,
        authorId = "lk452",
        createdAt = today.atStartOfDay().minusDays(daysAgo).plusHours(12),
        dueDate = due,
        pinnedUntil = pinnedUntil,
        color = NoteColor.Yellow,
        tack = Tack.DotRed,
    )

    @Test
    fun boardOrder_isPinnedThenDueSoonThenNewest() {
        val notes = listOf(
            note("newest", 0),
            note("older-plain", 3),
            note("due-later", 5, due = today.plusDays(10)),
            note("due-soon", 6, due = today.plusDays(1)),
            note("pinned", 9, pinnedUntil = today.plusDays(2)),
        )
        val order = NoteRules.boardOrder(notes, today).map { it.id }
        assertEquals(listOf("pinned", "due-soon", "due-later", "newest", "older-plain"), order)
    }

    @Test
    fun expiredPinsAndPastDatesDoNotCount() {
        val n = note("n", 2, due = today.minusDays(1), pinnedUntil = today.minusDays(1))
        assertEquals(false, n.isPinned(today))
        assertEquals(false, n.hasUpcomingDate(today))
        assertEquals(0, NoteRules.pinsInUse(listOf(n), today))
    }

    @Test
    fun unpinnedUndatedNotesMoveToOlderAfter14Days() {
        val fresh = note("fresh", 14)
        val stale = note("stale", 15)
        val staleButPinned = note("pinned", 30, pinnedUntil = today)
        val staleButDated = note("dated", 30, due = today.plusDays(3))
        val all = listOf(fresh, stale, staleButPinned, staleButDated)
        assertEquals(listOf("stale"), NoteRules.older(all, today).map { it.id })
        assertTrue(NoteRules.boardOrder(all, today).none { it.id == "stale" })
    }

    @Test
    fun homePreview_showsAllWhenSixOrFewer() {
        val notes = (1..6).map { note("n$it", it.toLong()) }
        val (shown, more) = NoteRules.homePreview(notes, today)
        assertEquals(6, shown.size)
        assertEquals(0, more)
    }

    @Test
    fun homePreview_foldsExtrasIntoMoreStack() {
        val notes = (1..12).map { note("n$it", it.toLong().coerceAtMost(10)) }
        val (shown, more) = NoteRules.homePreview(notes, today)
        assertEquals(5, shown.size)
        assertEquals(7, more)
    }

    @Test
    fun comingUp_isNextTwoDatedNotesSoonestFirst() {
        val notes = listOf(
            note("far", 1, due = today.plusDays(20)),
            note("undated", 0),
            note("soon", 1, due = today.plusDays(2)),
            note("today", 1, due = today),
            note("past", 1, due = today.minusDays(1)),
        )
        assertEquals(listOf("today", "soon"), NoteRules.comingUp(notes, today).map { it.id })
    }
}
