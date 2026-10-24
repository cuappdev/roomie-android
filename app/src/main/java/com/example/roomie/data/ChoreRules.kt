package com.example.roomie.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Behaviour rules for chores and weekly rotations, from the Chores section of the Figma file. */
object ChoreRules {
    const val MAX_PEOPLE_PER_WEEK = 3

    /** Chore weeks run Monday to Sunday, matching the M T W T F S S week strip. */
    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun weekDays(weekStart: LocalDate): List<LocalDate> = (0L..6L).map { weekStart.plusDays(it) }

    fun rotationChoreId(rotationId: String, week: LocalDate) = "rot_${rotationId}_$week"

    fun rotationChore(state: RoomieState, rotation: Rotation, week: LocalDate): Chore? {
        if (week.isBefore(rotation.startWeek)) return null
        val assignees = rotation.assigneesFor(week).ifEmpty { return null }
        val id = rotationChoreId(rotation.id, week)
        return Chore(
            id = id,
            title = rotation.title,
            assigneeIds = assignees,
            dueDate = week.plusDays(rotation.dueDay.value - 1L),
            completedOn = state.rotationCompletions[id],
            rotationId = rotation.id,
        )
    }

    /**
     * Chores due in the week starting [weekStart]. Viewing the current week also pulls in anything
     * still undone from earlier weeks: an assignment stays with its people until it's completed.
     */
    fun choresInWeek(state: RoomieState, weekStart: LocalDate, today: LocalDate): List<Chore> {
        val days = weekDays(weekStart)
        val oneOffs = state.chores.filter { it.dueDate in days.first()..days.last() }
        val weekly = state.rotations.mapNotNull { rotationChore(state, it, weekStart) }
        val carried = if (weekStart == weekStart(today)) overdue(state, weekStart) else emptyList()
        return (carried + oneOffs + weekly).sortedWith(compareBy<Chore> { it.done }.thenBy { it.dueDate })
    }

    /** Undone one-offs from before [weekStart], plus last week's rotation turns nobody finished. */
    private fun overdue(state: RoomieState, weekStart: LocalDate): List<Chore> {
        val oneOffs = state.chores.filter { !it.done && it.dueDate.isBefore(weekStart) }
        val lastWeek = weekStart.minusWeeks(1)
        val weekly = state.rotations.mapNotNull { rotationChore(state, it, lastWeek) }.filterNot { it.done }
        return oneOffs + weekly
    }

    fun choreById(state: RoomieState, id: String): Chore? {
        state.chores.firstOrNull { it.id == id }?.let { return it }
        val parts = id.split('_')
        if (parts.size != 3 || parts[0] != "rot") return null
        val rotation = state.rotations.firstOrNull { it.id == parts[1] } ?: return null
        val week = runCatching { LocalDate.parse(parts[2]) }.getOrNull() ?: return null
        return rotationChore(state, rotation, week)
    }

    /** The first week after this one, with its assignees, for the "Next: Leah · Nov 23" line. */
    fun nextTurn(rotation: Rotation, today: LocalDate): Pair<LocalDate, List<String>> {
        val week = maxOf(weekStart(today).plusWeeks(1), rotation.startWeek)
        return week to rotation.assigneesFor(week)
    }

    /**
     * The rotation as it would be with a new plan from [fromWeek]. The new member order starts with
     * whoever was due next, so an edit doesn't send the chore back to the first person.
     */
    fun withPlan(rotation: Rotation, fromWeek: LocalDate, memberIds: List<String>, peoplePerWeek: Int): Rotation {
        val upNext = rotation.assigneesFor(fromWeek).firstOrNull()
        val start = memberIds.indexOf(upNext).coerceAtLeast(0)
        val ordered = memberIds.drop(start) + memberIds.take(start)
        val kept = rotation.plans.filter { it.fromWeek.isBefore(fromWeek) }
        return rotation.copy(plans = kept + RotationPlan(fromWeek, ordered, peoplePerWeek))
    }
}
