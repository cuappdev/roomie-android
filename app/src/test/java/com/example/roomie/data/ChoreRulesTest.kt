package com.example.roomie.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ChoreRulesTest {
    // Thursday, so the week runs Mon Nov 16 – Sun Nov 22 like the Figma frames.
    private val today = LocalDate.of(2026, 11, 19)
    private val week = LocalDate.of(2026, 11, 16)

    private fun rotation(members: List<String>, perWeek: Int = 1, start: LocalDate = week) =
        Rotation("r", "Take out recycling", DayOfWeek.THURSDAY, listOf(RotationPlan(start, members, perWeek)))

    @Test
    fun weekStartsOnMonday() {
        assertEquals(week, ChoreRules.weekStart(today))
        assertEquals(week, ChoreRules.weekStart(week.plusDays(6)))
    }

    @Test
    fun rotationHandsOffOnePersonPerWeek() {
        val r = rotation(listOf("mina", "leah", "mihili"))
        assertEquals(listOf("mina"), r.assigneesFor(week))
        assertEquals(listOf("leah"), r.assigneesFor(week.plusWeeks(1)))
        assertEquals(listOf("mihili"), r.assigneesFor(week.plusWeeks(2)))
        assertEquals(listOf("mina"), r.assigneesFor(week.plusWeeks(3)))
    }

    @Test
    fun twoPeoplePerWeekMatchesFigmaPreview() {
        // C5: Nov 16 Mina + Leah, Nov 23 Mihili + Mina, Nov 30 Leah + Mihili.
        val r = rotation(listOf("mina", "leah", "mihili"), perWeek = 2)
        assertEquals(listOf("mina", "leah"), r.assigneesFor(week))
        assertEquals(listOf("mihili", "mina"), r.assigneesFor(week.plusWeeks(1)))
        assertEquals(listOf("leah", "mihili"), r.assigneesFor(week.plusWeeks(2)))
    }

    @Test
    fun rotationChoreIsDueOnItsWeekday() {
        val state = RoomieState(rotations = listOf(rotation(listOf("mina"))))
        val chore = ChoreRules.choresInWeek(state, week, today).single()
        assertEquals(LocalDate.of(2026, 11, 19), chore.dueDate)
        assertEquals("r", chore.rotationId)
        assertEquals(chore, ChoreRules.choreById(state, chore.id))
    }

    @Test
    fun noTurnsBeforeTheStartingWeek() {
        val state = RoomieState(rotations = listOf(rotation(listOf("mina"), start = week.plusWeeks(1))))
        assertTrue(ChoreRules.choresInWeek(state, week, today).isEmpty())
    }

    @Test
    fun unfinishedTurnCarriesIntoThisWeek() {
        val r = rotation(listOf("mina", "leah"), start = week.minusWeeks(1))
        val lastWeekId = ChoreRules.rotationChoreId("r", week.minusWeeks(1))
        val undone = ChoreRules.choresInWeek(RoomieState(rotations = listOf(r)), week, today)
        assertEquals(setOf(lastWeekId, ChoreRules.rotationChoreId("r", week)), undone.map { it.id }.toSet())

        val finished = RoomieState(rotations = listOf(r), rotationCompletions = mapOf(lastWeekId to week.minusDays(3)))
        assertEquals(listOf(ChoreRules.rotationChoreId("r", week)), ChoreRules.choresInWeek(finished, week, today).map { it.id })
    }

    @Test
    fun editsOnlyChangeFutureWeeksAndKeepWhoIsNext() {
        val r = rotation(listOf("mina", "leah", "mihili"))
        val nextWeek = week.plusWeeks(1)
        // Leah is due next; adding Stef shouldn't send it back to Mina.
        val edited = ChoreRules.withPlan(r, nextWeek, listOf("mina", "leah", "mihili", "stef"), 1)
        assertEquals(listOf("mina"), edited.assigneesFor(week))
        assertEquals(listOf("leah"), edited.assigneesFor(nextWeek))
        assertEquals(listOf("mihili"), edited.assigneesFor(nextWeek.plusWeeks(1)))
        assertEquals(listOf("stef"), edited.assigneesFor(nextWeek.plusWeeks(2)))
    }

    @Test
    fun unknownChoreIdIsNull() {
        assertNull(ChoreRules.choreById(RoomieState(), "rot_missing_2026-11-16"))
        assertNull(ChoreRules.choreById(RoomieState(), "nope"))
    }
}
