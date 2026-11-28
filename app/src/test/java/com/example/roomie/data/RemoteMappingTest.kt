package com.example.roomie.data

import com.example.roomie.data.api.ChoreDto
import com.example.roomie.data.api.ChoreFrequency
import com.example.roomie.data.api.Presence
import com.example.roomie.data.api.TurnChoreDto
import com.example.roomie.data.api.TurnDto
import com.example.roomie.data.api.TurnStatus
import com.example.roomie.data.api.UserDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RemoteMappingTest {

    @Test
    fun quantityRoundTripsThroughTheName() {
        assertEquals("Eggs ×2", RemoteMapping.encodeItemName("Eggs", 2))
        assertEquals("Eggs", RemoteMapping.encodeItemName(" Eggs ", 1))
        assertEquals("Eggs" to 2, RemoteMapping.decodeItemName("Eggs ×2"))
        assertEquals("Eggs" to 1, RemoteMapping.decodeItemName("Eggs"))
        // Not ours: a name that happens to end in ×1, or has no space before ×.
        assertEquals("Thing ×1" to 1, RemoteMapping.decodeItemName("Thing ×1"))
        assertEquals("Size×2" to 1, RemoteMapping.decodeItemName("Size×2"))
    }

    @Test
    fun encodedNamesFitTheServerLimit() {
        val encoded = RemoteMapping.encodeItemName("x".repeat(100), 12)
        assertEquals(80, encoded.length)
        assertEquals("x".repeat(76) to 12, RemoteMapping.decodeItemName(encoded))
    }

    @Test
    fun splitsNamesOnTheFirstSpace() {
        assertEquals("Stef" to "Rivera-Osorio", RemoteMapping.splitName("Stef Rivera-Osorio"))
        assertEquals("Mary" to "Ann Lee", RemoteMapping.splitName("Mary  Ann Lee"))
        assertEquals("mnh49" to "", RemoteMapping.splitName("mnh49"))
    }

    @Test
    fun acceptsTheBackendsNetIds() {
        assertEquals(true, RemoteMapping.isValidNetId("jw2877@cornell.edu"))
        assertEquals(true, RemoteMapping.isValidNetId("a1"))
        assertEquals(false, RemoteMapping.isValidNetId("abcde1"))
    }

    @Test
    fun firstTurnNeverStartsInThePast() {
        val wednesday = LocalDate.of(2026, 11, 25)
        val thisMonday = LocalDate.of(2026, 11, 23)
        assertEquals(LocalDate.of(2026, 11, 26), RemoteMapping.firstTurn(thisMonday, DayOfWeek.THURSDAY, wednesday))
        assertEquals(LocalDate.of(2026, 11, 30), RemoteMapping.firstTurn(thisMonday, DayOfWeek.MONDAY, wednesday))
        assertEquals(wednesday, RemoteMapping.firstTurn(thisMonday, DayOfWeek.WEDNESDAY, wednesday))
    }

    @Test
    fun rotationPredictsTheServersNextTurns() {
        val a = user(1); val b = user(2); val c = user(3)
        // B has the next turn, on Thursday Dec 3.
        val chore = ChoreDto(
            id = 9, title = "Trash", frequency = ChoreFrequency.WEEKLY, startsOn = LocalDate.of(2026, 11, 5),
            allowSwaps = true, reminderHour = null, rotation = listOf(a, b, c),
            comingUp = listOf(turn(40, b, LocalDate.of(2026, 12, 3))),
        )

        val rotation = RemoteMapping.rotation(chore)

        assertEquals(DayOfWeek.THURSDAY, rotation.dueDay)
        val week = ChoreRules.weekStart(LocalDate.of(2026, 12, 3))
        assertEquals(listOf("2"), rotation.assigneesFor(week))
        assertEquals(listOf("3"), rotation.assigneesFor(week.plusWeeks(1)))
        assertEquals(listOf("1"), rotation.assigneesFor(week.plusWeeks(2)))
    }

    @Test
    fun turnsBecomeChoresWithPrefixedIds() {
        val chore = RemoteMapping.chore(turn(40, user(2), LocalDate.of(2026, 12, 3)), java.time.ZoneId.of("America/New_York"))
        assertEquals("t40", chore.id)
        assertEquals("9", chore.rotationId)
        assertEquals(listOf("2"), chore.assigneeIds)
    }

    @Test
    fun serverTurnsAreNotDoubledByRotations() {
        val week = LocalDate.of(2026, 11, 23)
        val rotation = Rotation("9", "Trash", DayOfWeek.THURSDAY, listOf(RotationPlan(week, listOf("2"), 1)))
        val turn = Chore("t40", "Trash", listOf("2"), week.plusDays(3), rotationId = "9")
        val state = RoomieState(chores = listOf(turn), rotations = listOf(rotation), capabilities = Capabilities.Backend)

        assertEquals(listOf("t40"), ChoreRules.choresInWeek(state, week, week).map { it.id })
    }

    private fun user(id: Int) = UserDto(id, "u$id", "User $id", null, Presence.HOME)

    private fun turn(id: Int, assignee: UserDto, dueOn: LocalDate) = TurnDto(
        id = id, chore = TurnChoreDto(9, "Trash", ChoreFrequency.WEEKLY), dueOn = dueOn, status = TurnStatus.UPCOMING,
        assignee = assignee, completedAt = null, completedBy = null, canSwap = true,
    )
}
