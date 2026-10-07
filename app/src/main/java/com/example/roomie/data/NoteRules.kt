package com.example.roomie.data

import java.time.LocalDate

/** Behaviour rules for notes, from the "Behaviour notes" frame in the Figma file. */
object NoteRules {
    const val MAX_LENGTH = 140
    const val MAX_PINS = 3
    const val PIN_DAYS = 7L
    const val OLDER_AFTER_DAYS = 14L
    const val HOME_SPOTS = 6
    const val COMING_UP_COUNT = 2

    /** Unpinned notes with no upcoming date move to "Older notes" after 14 days. */
    fun isOlder(note: Note, today: LocalDate): Boolean =
        !note.isPinned(today) &&
            !note.hasUpcomingDate(today) &&
            note.createdAt.toLocalDate().isBefore(today.minusDays(OLDER_AFTER_DAYS))

    /** Pinned first, then due soon, then newest. Excludes older notes. */
    fun boardOrder(notes: List<Note>, today: LocalDate): List<Note> {
        val active = notes.filterNot { isOlder(it, today) }
        val pinned = active.filter { it.isPinned(today) }.sortedByDescending { it.createdAt }
        val dated = active.filter { !it.isPinned(today) && it.hasUpcomingDate(today) }.sortedBy { it.dueDate }
        val rest = active.filter { !it.isPinned(today) && !it.hasUpcomingDate(today) }.sortedByDescending { it.createdAt }
        return pinned + dated + rest
    }

    fun older(notes: List<Note>, today: LocalDate): List<Note> =
        notes.filter { isOlder(it, today) }.sortedByDescending { it.createdAt }

    /**
     * Home shows at most 6 note spots. When there are more notes than spots, the last spot
     * becomes a "+N more" stack that opens the full board.
     */
    fun homePreview(notes: List<Note>, today: LocalDate): Pair<List<Note>, Int> {
        val ordered = boardOrder(notes, today)
        if (ordered.size <= HOME_SPOTS) return ordered to 0
        val shown = ordered.take(HOME_SPOTS - 1)
        return shown to ordered.size - shown.size
    }

    /** "Coming up" on Home is fed only by dated notes: the next 2, soonest first. */
    fun comingUp(notes: List<Note>, today: LocalDate): List<Note> =
        notes.filter { it.hasUpcomingDate(today) }
            .sortedWith(compareBy({ it.dueDate }, { it.createdAt }))
            .take(COMING_UP_COUNT)

    fun pinsInUse(notes: List<Note>, today: LocalDate): Int = notes.count { it.isPinned(today) }
}
