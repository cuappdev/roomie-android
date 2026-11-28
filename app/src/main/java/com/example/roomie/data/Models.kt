package com.example.roomie.data

import androidx.annotation.DrawableRes
import com.example.roomie.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** The "house buddy" avatar each roommate picks during onboarding. */
enum class Buddy(val label: String, @param:DrawableRes val drawable: Int) {
    Mug("Mug", R.drawable.avatar_mug),
    Sock("Sock", R.drawable.avatar_sock),
    Plant("Plant", R.drawable.avatar_plant),
    Toaster("Toaster", R.drawable.avatar_toaster),
    Teapot("Teapot", R.drawable.avatar_teapot),
}

enum class MemberStatus { Owner, Joined, InviteSent }

data class Roommate(
    val id: String,
    val netId: String,
    val firstName: String,
    val lastName: String = "",
    val buddy: Buddy,
    val status: MemberStatus,
) {
    val fullName: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
}

data class House(
    val name: String,
    val inviteCode: String,
    val members: List<Roommate>,
    /** Roommate currently shopping, shown as a banner on the shopping list. */
    val atStoreId: String? = null,
)

enum class NoteColor { Yellow, Peach, Green, Pink, Purple, Blue }

enum class Tack { StarOrange, StarPink, DotGreen, DotRed, DotBlue }

data class Note(
    val id: String,
    val text: String,
    val authorId: String,
    val createdAt: LocalDateTime,
    val dueDate: LocalDate? = null,
    /** Pins last 7 days; null when not pinned. */
    val pinnedUntil: LocalDate? = null,
    val color: NoteColor,
    val tack: Tack,
) {
    fun isPinned(today: LocalDate): Boolean = pinnedUntil != null && !pinnedUntil.isBefore(today)
    fun hasUpcomingDate(today: LocalDate): Boolean = dueDate != null && !dueDate.isBefore(today)
}

data class Chore(
    val id: String,
    val title: String,
    val assigneeIds: List<String>,
    val dueDate: LocalDate,
    val completedOn: LocalDate? = null,
    /** Set when this chore is one week of a [Rotation]. */
    val rotationId: String? = null,
) {
    val done: Boolean get() = completedOn != null
}

/** Who a rotation includes and how many of them take it each week, from [fromWeek] (a Monday) on. */
data class RotationPlan(val fromWeek: LocalDate, val memberIds: List<String>, val peoplePerWeek: Int)

/**
 * A chore that repeats weekly and hands itself to the next people in line. Edits add a new plan
 * starting the following week, so past and current weeks keep their assignees.
 */
data class Rotation(
    val id: String,
    val title: String,
    val dueDay: DayOfWeek,
    /** Oldest first. */
    val plans: List<RotationPlan>,
) {
    val startWeek: LocalDate get() = plans.first().fromWeek

    fun planFor(week: LocalDate): RotationPlan? = plans.lastOrNull { !it.fromWeek.isAfter(week) }

    fun assigneesFor(week: LocalDate): List<String> {
        val plan = planFor(week) ?: return emptyList()
        val members = plan.memberIds
        if (members.isEmpty()) return emptyList()
        val perWeek = plan.peoplePerWeek.coerceIn(1, members.size)
        val index = ChronoUnit.WEEKS.between(plan.fromWeek, week).toInt()
        return (0 until perWeek).map { members[(index * perWeek + it) % members.size] }
    }
}

data class ShoppingItem(
    val id: String,
    val name: String,
    val quantity: Int = 1,
    val note: String = "",
    val addedById: String,
    val addedAt: LocalDateTime,
    val boughtById: String? = null,
    val boughtAt: LocalDateTime? = null,
    val couldntFindById: String? = null,
) {
    val isBought: Boolean get() = boughtAt != null
}

data class Session(
    val userId: String,
    val netId: String,
    val firstName: String,
    val lastName: String,
    val buddy: Buddy,
    /** True once the user belongs to a house; false sends them through onboarding. */
    val hasHouse: Boolean,
)

/**
 * What the data source behind [RoomieState] can do. The in-memory fake does everything the design
 * describes; the real backend doesn't support a few things yet, so screens hide those controls.
 */
data class Capabilities(
    /** "Assign Once" chores. The backend only has repeating chores. */
    val oneOffChores: Boolean = true,
    /** The backend gives each turn to one person. */
    val maxPeoplePerWeek: Int = ChoreRules.MAX_PEOPLE_PER_WEEK,
    /** Picking the week a rotation edit starts from. The backend applies edits from the next turn. */
    val rotationStartWeek: Boolean = true,
    /** A free-text note on a shopping item. */
    val itemNotes: Boolean = true,
    /** "Couldn't find it" on a shopping item. */
    val couldntFind: Boolean = true,
    /**
     * True when the server sends every turn, rotation weeks included, in [RoomieState.chores].
     * False when [ChoreRules] works rotation weeks out from [RoomieState.rotations] (the fake).
     */
    val turnsFromServer: Boolean = false,
) {
    companion object {
        /** The Roomie backend as of its `mvp-features` branch. */
        val Backend = Capabilities(
            oneOffChores = false,
            maxPeoplePerWeek = 1,
            rotationStartWeek = false,
            itemNotes = false,
            couldntFind = false,
            turnsFromServer = true,
        )
    }
}

data class RoomieState(
    val session: Session? = null,
    val house: House? = null,
    val notes: List<Note> = emptyList(),
    /** One-off chores; weekly ones come from [rotations]. */
    val chores: List<Chore> = emptyList(),
    val rotations: List<Rotation> = emptyList(),
    /** Completion date of each rotation week, keyed by [ChoreRules.rotationChoreId]. */
    val rotationCompletions: Map<String, LocalDate> = emptyMap(),
    val shopping: List<ShoppingItem> = emptyList(),
    val capabilities: Capabilities = Capabilities(),
    /** True while a saved sign-in is being checked on launch, before the first screen is picked. */
    val restoring: Boolean = false,
) {
    fun member(id: String?): Roommate? = house?.members?.firstOrNull { it.id == id }
}
